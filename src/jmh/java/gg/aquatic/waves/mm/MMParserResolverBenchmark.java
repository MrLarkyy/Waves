package gg.aquatic.waves.mm;

import gg.aquatic.waves.mm.tag.resolver.MMTagResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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
public class MMParserResolverBenchmark {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Param({
        "parsed",
        "unparsed",
        "styling",
        "number",
        "joining"
    })
    public String scenario;

    private String input;
    private TagResolver miniResolver;
    private MMTagResolver mmResolver;

    @Setup
    public void setup() {
        switch (scenario) {
            case "parsed" -> {
                input = "Hello <name>!";
                miniResolver = Placeholder.parsed("name", "<red>Bob</red>");
                mmResolver = MMPlaceholder.parsed("name", "<red>Bob</red>");
            }
            case "unparsed" -> {
                input = "Hello <name>!";
                miniResolver = Placeholder.unparsed("name", "<red>Bob</red>");
                mmResolver = MMPlaceholder.unparsed("name", "<red>Bob</red>");
            }
            case "styling" -> {
                input = "<fancy>Hello</fancy> world";
                miniResolver = Placeholder.styling("fancy", NamedTextColor.RED, TextDecoration.BOLD);
                mmResolver = MMPlaceholder.styling("fancy", NamedTextColor.RED, TextDecoration.BOLD);
            }
            case "number" -> {
                input = "Balance: <no:'en-US':'#.00'>";
                miniResolver = Formatter.number("no", 250.25d);
                mmResolver = MMFormatter.number("no", 250.25d);
            }
            case "joining" -> {
                input = "Items: <items:'<gray>, </gray>':'<gray> and </gray>'>";
                miniResolver = Formatter.joining(
                    "items",
                    Component.text("A"),
                    Component.text("B"),
                    Component.text("C")
                );
                mmResolver = MMFormatter.joining(
                    "items",
                    Component.text("A"),
                    Component.text("B"),
                    Component.text("C")
                );
            }
            default -> throw new IllegalStateException("Unknown scenario: " + scenario);
        }
    }

    @Benchmark
    public void miniMessage(Blackhole blackhole) {
        blackhole.consume(miniMessage.deserialize(input, miniResolver));
    }

    @Benchmark
    public void mmParser(Blackhole blackhole) {
        blackhole.consume(MMParser.deserialize(input, mmResolver));
    }
}
