package com.roiocam.plugin;


import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import com.roiocam.plugin.tools.LibraryScope;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;

/**
 * A base mojo filtering the dependencies of the project.
 *
 * @author Stephane Nicoll
 * @author David Turanski
 * @since 1.1.0
 */
public abstract class AbstractDependencyFilterMojo extends AbstractMojo {

    /**
     * Collection of artifact definitions to include. The {@link Include} element defines
     * a {@code groupId} and {@code artifactId} mandatory properties and an optional
     * {@code classifier} property.
     * @since 1.2.0
     */
    @Parameter(property = "spring-boot.includes")
    private List<Include> includes;

    /**
     * Collection of artifact definitions to exclude. The {@link Exclude} element defines
     * a {@code groupId} and {@code artifactId} mandatory properties and an optional
     * {@code classifier} property.
     * @since 1.1.0
     */
    @Parameter(property = "spring-boot.excludes")
    private List<Exclude> excludes;

    /**
     * Comma separated list of groupId names to exclude (exact match).
     * @since 1.1.0
     */
    @Parameter(property = "spring-boot.excludeGroupIds", defaultValue = "")
    private String excludeGroupIds;

    /**
     * Comma separated list of artifact names to exclude (exact match).
     * @since 1.1.0
     * @deprecated as of 2.0.2 in favour of {@code excludes}
     */
    @Parameter(property = "spring-boot.excludeArtifactIds", defaultValue = "")
    @Deprecated
    private String excludeArtifactIds;

    protected void setExcludes(List<Exclude> excludes) {
        this.excludes = excludes;
    }

    protected void setIncludes(List<Include> includes) {
        this.includes = includes;
    }

    protected void setExcludeGroupIds(String excludeGroupIds) {
        this.excludeGroupIds = excludeGroupIds;
    }

    @Deprecated
    protected void setExcludeArtifactIds(String excludeArtifactIds) {
        this.excludeArtifactIds = excludeArtifactIds;
    }

    protected Set<Artifact> filterDependencies(Set<Artifact> dependencies, FilterArtifacts filters)
            throws MojoExecutionException {
        try {
            Set<Artifact> filtered = new LinkedHashSet<>(dependencies);
            filtered.retainAll(filters.filter(dependencies));
            for (Artifact dependency : dependencies) {
                if (dependency.getScope().equals(LibraryScope.CUSTOM.toString())) {
                    filtered.add(dependency);
                }
            }
            return filtered;
        }
        catch (ArtifactFilterException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    /**
     * Return artifact filters configured for this MOJO.
     * @param additionalFilters optional additional filters to apply
     * @return the filters
     */
    protected final FilterArtifacts getFilters(ArtifactsFilter... additionalFilters) {
        FilterArtifacts filters = new FilterArtifacts();
        for (ArtifactsFilter additionalFilter : additionalFilters) {
            filters.addFilter(additionalFilter);
        }
        filters.addFilter(new ArtifactIdFilter("", cleanFilterConfig(this.excludeArtifactIds)));
        filters.addFilter(new MatchingGroupIdFilter(cleanFilterConfig(this.excludeGroupIds)));
        if (this.includes != null && !this.includes.isEmpty()) {
            filters.addFilter(new IncludeFilter(this.includes));
        }
        if (this.excludes != null && !this.excludes.isEmpty()) {
            filters.addFilter(new ExcludeFilter(this.excludes));
        }
        return filters;
    }

    private String cleanFilterConfig(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        StringBuilder cleaned = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(content, ",");
        while (tokenizer.hasMoreElements()) {
            cleaned.append(tokenizer.nextToken().trim());
            if (tokenizer.hasMoreElements()) {
                cleaned.append(",");
            }
        }
        return cleaned.toString();
    }

}
