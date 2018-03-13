import com.sun.xml.internal.ws.Closeable;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ScoringFunction implements Closeable {
    private Socket sockd;
    private List<String> names;
    private Scanner sock_in;
    private PrintWriter sock_out;

    public ScoringFunction(List<String> names, int port) throws IOException {
        sockd = new Socket("localhost", port);
        this.names = names;
        sock_in = new Scanner(sockd.getInputStream());
        sock_out = new PrintWriter(sockd.getOutputStream());
    }

    public double score(int target, List<Integer> parents) {
        String target_str = names.get(target);
        String str = parents.stream()
                .map(x -> names.get(x))
                .collect(Collectors.joining("\t"));
        sock_out.println(target_str + "\t" + str);
        sock_out.flush();
        return sock_in.nextDouble();
    }

    @Override
    public void close() {
        try {
            sockd.close();
        } catch (IOException e) {
            System.err.println("AHTUNG!");
        }
    }
}
