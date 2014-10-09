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
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.inject.AbstractModule;

public class FilteredArtifactResolver implements ArtifactResolver {

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
    validate(session, Collections.singletonList(result));
    return result;
  }

  @Override
  public List<ArtifactResult> resolveArtifacts(RepositorySystemSession session,
      Collection<? extends ArtifactRequest> requests) throws ArtifactResolutionException {
    List<ArtifactResult> results = resolver.resolveArtifacts(session, requests);
    validate(session, results);
    return results;
  }

  private void validate(RepositorySystemSession session, List<ArtifactResult> results)
      throws ArtifactResolutionException {
    TakariTargetPlatform targetPlatform =
        (TakariTargetPlatform) session.getData().get(TakariTargetPlatform.class);

    if (targetPlatform != null) {
      List<Artifact> blocked = new ArrayList<Artifact>();
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
          log.error("Artifact is not part of the project build target platform {}", artifact);
          blocked.add(artifact);
        } else {
          try {
            String actualSha1 = Files.hash(artifact.getFile(), Hashing.sha1()).toString();
            String expectedSha1 = targetPlatform.getSHA1(artifact);
            if (!actualSha1.equals(expectedSha1)) {
              log.error("Artifact {} has invalid SHA1 checksum, expected {}, actual {}", artifact,
                  expectedSha1, actualSha1);
              blocked.add(artifact);
            }
          } catch (IOException e) {
            log.error("Could not calculate artifact SHA1 checksum {}", artifact, e);
            blocked.add(artifact);
          }
        }
      }
      if (!blocked.isEmpty()) {
        throw new ArtifactResolutionException(results,
            "Artifacts are not part of the project build target platform " + blocked);
      }
    }
  }

  @Named
  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      bind(ArtifactResolver.class).to(FilteredArtifactResolver.class);
    }
  }
}
