package io.takari.maven.targetplatform;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import com.google.inject.AbstractModule;

public class FilteredArtifactResolver
    implements ArtifactResolver
{

    private final DefaultArtifactResolver resolver;

    @Inject
    public FilteredArtifactResolver( DefaultArtifactResolver resolver )
    {
        this.resolver = resolver;
    }

    @Override
    public ArtifactResult resolveArtifact( RepositorySystemSession session, ArtifactRequest request )
        throws ArtifactResolutionException
    {
        return resolver.resolveArtifact( session, request );
    }

    @Override
    public List<ArtifactResult> resolveArtifacts( RepositorySystemSession session,
                                                  Collection<? extends ArtifactRequest> requests )
        throws ArtifactResolutionException
    {
        return resolver.resolveArtifacts( session, requests );
    }

    @Named
    public static class Module
        extends AbstractModule
    {
        @Override
        protected void configure()
        {
            bind( ArtifactResolver.class ).to( FilteredArtifactResolver.class );
        }
    }
}
