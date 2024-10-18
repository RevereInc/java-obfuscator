package dev.revere.obfuscator;

import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.exception.ObfuscationException;
import dev.revere.obfuscator.logging.Logger;
import dev.revere.obfuscator.logging.LoggerConfig;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
@Command(name = "obfuscator", mixinStandardHelpOptions = true, version = "1.0")
public class Main implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    @Parameters(index = "0", description = "Input jar file to obfuscate")
    private File inputFile;

    @Option(names = {"-o", "--output"}, description = "Path to the output jar location", required = true)
    private File outputFile;

    @Option(names = {"-cfg", "--config"}, description = "Path to the config file", required = true)
    private File configFile;

    @Option(names = {"-li", "--libs"}, description = "Path to the libs folder", required = false)
    private File libsFolder;

    @Override
    public Integer call() {
        try {
            LOGGER.process("Starting obfuscation process...");
            Configuration config = Configuration.loadFromFile(Paths.get(configFile.getAbsolutePath()).toString());

            ObfuscationEngine obfuscationEngine = new ObfuscationEngine();
            obfuscationEngine.obfuscate(inputFile.toPath(), outputFile.toPath(), config);

            LOGGER.success("Obfuscation completed successfully!");
            return 0;
        } catch (IOException | ObfuscationException e) {
            LOGGER.severe("Error: " + e.getMessage());
            return 1;
        }
    }

    public static void main(String[] args) {
        LoggerConfig.configureLogger();
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}