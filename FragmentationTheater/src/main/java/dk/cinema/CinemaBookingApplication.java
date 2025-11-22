package dk.cinema;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dk.cinema.controller.BookingController;
import dk.cinema.service.BookingService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class CinemaBookingApplication {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", new StaticFileHandler("web"));

        BookingService bookingService = new BookingService();
        BookingController controller = new BookingController(bookingService);

        server.createContext("/api/cinema", controller::handleCinemaState);
        server.createContext("/api/book", controller::handleBooking);
        server.createContext("/api/reset", controller::handleReset);
        server.createContext("/api/stats", controller::handleStats);
        server.createContext("/api/available-seats", controller::handleAvailableSeats);
        server.createContext("/api/config", controller::handleConfig);

        server.setExecutor(null);
        server.start();

        System.out.println("Cinema Booking API + Frontend running on http://localhost:8080");
    }

    static class StaticFileHandler implements HttpHandler {
        private final Path baseDir;

        StaticFileHandler(String baseDir) {
            this.baseDir = Path.of(baseDir).toAbsolutePath();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String filePath = exchange.getRequestURI().getPath();
            if (filePath.equals("/")) filePath = "/index.html";

            Path file = baseDir.resolve("." + filePath).normalize();

            if (!file.startsWith(baseDir) || !Files.exists(file)) {
                exchange.sendResponseHeaders(404, 0);
                exchange.getResponseBody().close();
                return;
            }

            String mimeType = Files.probeContentType(file);
            if (mimeType == null) mimeType = "application/octet-stream";
            exchange.getResponseHeaders().set("Content-Type", mimeType);

            byte[] data = Files.readAllBytes(file);
            exchange.sendResponseHeaders(200, data.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }
    }
}
