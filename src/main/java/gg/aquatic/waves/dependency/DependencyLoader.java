package gg.aquatic.waves.dependency;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import org.slf4j.LoggerFactory;
import xyz.jpenilla.gremlin.runtime.DependencyCache;
import xyz.jpenilla.gremlin.runtime.DependencyResolver;
import xyz.jpenilla.gremlin.runtime.DependencySet;
import xyz.jpenilla.gremlin.runtime.logging.Slf4jGremlinLogger;
import xyz.jpenilla.gremlin.runtime.platformsupport.PaperClasspathAppender;

import java.nio.file.Path;
import java.util.Set;

@SuppressWarnings("unused")
public class DependencyLoader implements PluginLoader {

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        try {
            DependencySet deps = DependencySet.readDefault(this.getClass().getClassLoader());
            Path cacheDir = Path.of("plugins/Waves/dependencies");
            DependencyCache cache = new DependencyCache(cacheDir);

            try (DependencyResolver resolver = new DependencyResolver(new Slf4jGremlinLogger(LoggerFactory.getLogger("Waves")))) {
                PaperClasspathAppender appender = new PaperClasspathAppender(classpathBuilder);
                Set<Path> jars = resolver.resolve(deps, cache).jarFiles();
                for (Path jar : jars) {
                    appender.append(jar);
                }
            }

            // Optional: cleanup old cached files
            cache.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
