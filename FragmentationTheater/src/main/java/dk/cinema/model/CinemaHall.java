package dk.cinema.model;

import java.util.*;

public class CinemaHall {
    private final int rows;
    private final int seatsPerRow;
    private final Seat[][] seats;

    public CinemaHall(int rows, int seatsPerRow) {
        this.rows = rows;
        this.seatsPerRow = seatsPerRow;
        this.seats = new Seat[rows][seatsPerRow];

        for (int r = 0; r < rows; r++) {
            for (int s = 0; s < seatsPerRow; s++) {
                seats[r][s] = new Seat(r, s);
            }
        }
    }

    public Seat[][] getSeats() { return seats; }
    public int getRows() { return rows; }
    public int getSeatsPerRow() { return seatsPerRow; }

    public Seat getSeat(int row, int number) {
        if (row >= 0 && row < rows && number >= 0 && number < seatsPerRow) {
            return seats[row][number];
        }
        return null;
    }

    public List<Seat> getAllSeats() {
        List<Seat> allSeats = new ArrayList<>();
        for (Seat[] row : seats) {
            allSeats.addAll(Arrays.asList(row));
        }
        return allSeats;
    }
}