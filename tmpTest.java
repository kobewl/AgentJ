import reactor.core.publisher.Sinks;
public class tmpTest {
  public static void main(String[] args) {
    Sinks.One<String> sink = Sinks.one();
    System.out.println(sink.tryEmitValue("hello"));
    System.out.println(sink.tryEmitValue("again"));
  }
}
