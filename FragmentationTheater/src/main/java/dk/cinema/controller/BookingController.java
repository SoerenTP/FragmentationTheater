package dk.cinema.controller;

import com.sun.net.httpserver.HttpExchange;
import dk.cinema.model.*;
import dk.cinema.service.BookingService;
import org.json.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public void handleCinemaState(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        CinemaHall hall = bookingService.getCinemaHall();
        JSONObject response = new JSONObject();
        JSONArray rows = new JSONArray();

        for (int r = 0; r < hall.getRows(); r++) {
            JSONArray row = new JSONArray();
            for (int s = 0; s < hall.getSeatsPerRow(); s++) {
                Seat seat = hall.getSeat(r, s);
                JSONObject seatJson = new JSONObject();
                seatJson.put("id", seat.getId());
                seatJson.put("row", seat.getRow());
                seatJson.put("number", seat.getNumber());
                seatJson.put("occupied", seat.isOccupied());
                row.put(seatJson);
            }
            rows.put(row);
        }

        response.put("rows", rows);
        sendJsonResponse(exchange, 200, response.toString());
    }

    public void handleBooking(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject json = new JSONObject(body);

        BookingRequest request = new BookingRequest();
        request.setSeatIds(jsonArrayToList(json.getJSONArray("seatIds")));
        request.setCustomerName(json.optString("customerName", "Guest"));

        Map<String, Object> result = bookingService.bookSeats(request);
        JSONObject response = new JSONObject(result);

        sendJsonResponse(exchange, 200, response.toString());
    }

    public void handleReset(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        bookingService.reset();
        sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Cinema reset\"}");
    }

    public void handleStats(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        Map<String, Object> stats = bookingService.getStatistics();
        JSONObject response = new JSONObject(stats);
        sendJsonResponse(exchange, 200, response.toString());
    }

    public void handleAvailableSeats(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        int partySize = 1;

        if (query != null && query.startsWith("partySize=")) {
            try {
                partySize = Integer.parseInt(query.substring(10));
            } catch (NumberFormatException e) {
                partySize = 1;
            }
        }

        List<String> availableSeats = bookingService.getAvailableSeatsForBooking(partySize);

        JSONObject response = new JSONObject();
        response.put("partySize", partySize);
        response.put("availableSeats", new JSONArray(availableSeats));

        sendJsonResponse(exchange, 200, response.toString());
    }

    public void handleConfig(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        CinemaHall hall = bookingService.getCinemaHall();
        JSONObject response = new JSONObject();
        response.put("rows", hall.getRows());
        response.put("seatsPerRow", hall.getSeatsPerRow());
        response.put("totalSeats", hall.getRows() * hall.getSeatsPerRow());

        sendJsonResponse(exchange, 200, response.toString());
    }

    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private List<String> jsonArrayToList(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }
        return list;
    }
}
