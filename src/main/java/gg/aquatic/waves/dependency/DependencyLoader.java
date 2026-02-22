package gg.aquatic.waves.dependency;

import gg.aquatic.runtime.DependencyManager;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.JarLibrary;

import java.io.InputStream;
import java.nio.file.Path;

@SuppressWarnings("unused")
public class DependencyLoader implements PluginLoader {

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        try (InputStream in = getClass().getResourceAsStream("/dependencies.json")) {
            if (in == null) {
                throw new IllegalStateException("dependencies.json not found inside plugin jar");
            }

            Path baseDir = Path.of("plugins/Waves/dependencies");

            DependencyManager.create(baseDir)
                    .loadSecrets(Path.of("plugins/Waves/.env"))
                    .loadSecrets(Path.of(".env"))
                    .process(in, jar -> classpathBuilder.addLibrary(new JarLibrary(jar)));

            System.out.println("[DependencyLoader] Runtime dependencies loaded successfully.");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleError(Exception e) {
        System.err.println("CRITICAL: Dependency loading failed!");
        e.printStackTrace();
        System.exit(1);
    }
}
