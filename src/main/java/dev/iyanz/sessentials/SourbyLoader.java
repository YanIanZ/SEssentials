package dev.iyanz.sessentials;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Paper {@link PluginLoader} that pulls SEssentials' one runtime library — the SQLite
 * JDBC driver used by the CMI importer — from Maven Central at startup instead of shading
 * it into the plugin jar. This keeps the jar small (~a few MB instead of ~15 MB): the
 * ~12 MB driver is downloaded once into Paper's shared library cache and added to this
 * plugin's isolated classloader.
 *
 * <p>Declared via {@code loader: dev.iyanz.sessentials.SourbyLoader} in
 * {@code paper-plugin.yml}. The driver still registers normally — {@code CmiImporter}
 * loads it by name ({@code Class.forName("org.sqlite.JDBC")}) before opening a
 * connection, and that lookup resolves against this plugin's classloader.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class SourbyLoader implements PluginLoader {

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(new RemoteRepository.Builder(
                "central", "default", "https://repo1.maven.org/maven2/").build());
        resolver.addDependency(new Dependency(
                new DefaultArtifact("org.xerial:sqlite-jdbc:3.47.1.0"), null));
        classpathBuilder.addLibrary(resolver);
    }
}
