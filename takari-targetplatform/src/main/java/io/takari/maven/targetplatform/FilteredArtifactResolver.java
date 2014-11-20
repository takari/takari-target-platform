package io.takari.maven.targetplatform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.inject.AbstractModule;

public class FilteredArtifactResolver implements ArtifactResolver {

  @Named
  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      bind(ArtifactResolver.class).to(FilteredArtifactResolver.class);
    }
  }

  private Logger log = LoggerFactory.getLogger(getClass());

  private final DefaultArtifactResolver resolver;

  @Inject
  public FilteredArtifactResolver(DefaultArtifactResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public ArtifactResult resolveArtifact(RepositorySystemSession session, ArtifactRequest request)
      throws ArtifactResolutionException {
    ArtifactResult result = resolver.resolveArtifact(session, request);
    List<ArtifactResult> results = Collections.singletonList(result);
    List<ArtifactResult> blocked = validate(session, results);
    if (!blocked.isEmpty()) {
      Artifact artifact = blocked.get(0).getArtifact();
      ArtifactRepository repository = blocked.get(0).getRepository();
      RemoteRepository remoteRepository =
          (RemoteRepository) (repository instanceof RemoteRepository ? repository : null);
      String message = "Artifact is not part of the project build target platform " + artifact;
      ArtifactNotFoundException anfe = new ArtifactNotFoundException(artifact, remoteRepository);
      throw new ArtifactResolutionException(results, message, anfe);
    }
    return result;
  }

  @Override
  public List<ArtifactResult> resolveArtifacts(RepositorySystemSession session,
      Collection<? extends ArtifactRequest> requests) throws ArtifactResolutionException {
    List<ArtifactResult> results = resolver.resolveArtifacts(session, requests);
    List<ArtifactResult> blocked = validate(session, results);
    if (!blocked.isEmpty()) {
      StringBuilder message = new StringBuilder();
      message.append("Artifacts are not part of the project build target platform: [");
      for (int i = 0; i < blocked.size(); i++) {
        if (i > 0) {
          message.append(", ");
        }
        message.append(blocked.get(i).getArtifact());
      }
      message.append("]");
      throw new ArtifactResolutionException(results, message.toString());
    }
    return results;
  }

  private List<ArtifactResult> validate(RepositorySystemSession session,
      List<ArtifactResult> results) throws ArtifactResolutionException {
    TakariTargetPlatform targetPlatform =
        (TakariTargetPlatform) session.getData().get(TakariTargetPlatform.class);

    if (targetPlatform != null) {
      List<ArtifactResult> blocked = new ArrayList<>();
      for (ArtifactResult result : results) {
        Artifact artifact = result.getArtifact();

        if (artifact.getProperties().containsKey(ArtifactProperties.LOCAL_PATH)) {
          // do not validate system-scoped artifacts
          continue;
        }

        if (result.getRepository() instanceof WorkspaceRepository) {
          // less then ideal, the idea is to ignore artifacts resolved from reactor projects
          continue;
        }

        // there is no good way to propagate multiple reasons to the caller
        // have to use generic exception message and logging

        if (!targetPlatform.includes(artifact)) {
          log.info("Artifact is not part of the project build target platform {}", artifact);
          blocked.add(result);
        } else {
          try {
            String actualSha1 = Files.hash(artifact.getFile(), Hashing.sha1()).toString();
            String expectedSha1 = targetPlatform.getSHA1(artifact);
            if (!actualSha1.equals(expectedSha1)) {
              log.info("Artifact {} has invalid SHA1 checksum, expected {}, actual {}", artifact,
                  expectedSha1, actualSha1);
              blocked.add(result);
            }
          } catch (IOException e) {
            log.info("Could not calculate artifact SHA1 checksum {}", artifact, e);
            blocked.add(result);
          }
        }
      }
      return blocked;
    }
    return Collections.emptyList();
  }
}
