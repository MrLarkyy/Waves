package gg.aquatic.waves.mm;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class MMParserBenchmark {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Param({
        "plain",
        "simple",
        "nested",
        "gradient",
        "rainbow",
        "hover",
        "nbt",
        "translatable"
    })
    public String scenario;

    private String input;

    @Setup
    public void setup() {
        switch (scenario) {
            case "plain" -> input = "Hello world, this is a plain string with no tags.";
            case "simple" -> input = "<red>Hello</red> world";
            case "nested" -> input = "<bold><blue>Hello</blue> <italic>world</italic></bold>";
            case "gradient" -> input = "<gradient:#ff0000:#00ff00>" + longText() + "</gradient>";
            case "rainbow" -> input = "<rainbow>" + longText() + "</rainbow>";
            case "hover" -> input = "<hover:show_text:'<green>Hover</green>'>Hover me</hover>";
            case "nbt" -> input = "Value: <nbt:storage:minecraft:foo:bar>";
            case "translatable" -> input = "<lang:chat.type.text:'<green>A</green>':'<blue>B</blue>'>";
            default -> throw new IllegalStateException("Unknown scenario: " + scenario);
        }
    }

    @Benchmark
    public void miniMessage(Blackhole blackhole) {
        blackhole.consume(miniMessage.deserialize(input));
    }

    @Benchmark
    public void mmParser(Blackhole blackhole) {
        blackhole.consume(MMParser.deserialize(input));
    }

    private static String longText() {
        return "This is a longer benchmark string to exercise parsing. ".repeat(8);
    }
}
