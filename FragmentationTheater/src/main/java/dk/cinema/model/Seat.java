package dk.cinema.model;

public class Seat {
    private final int row;
    private final int number;
    private boolean occupied;
    private String bookingId;

    public Seat(int row, int number) {
        this.row = row;
        this.number = number;
        this.occupied = false;
    }

    public int getRow() { return row; }
    public int getNumber() { return number; }
    public boolean isOccupied() { return occupied; }
    public String getBookingId() { return bookingId; }

    public void book(String bookingId) {
        this.occupied = true;
        this.bookingId = bookingId;
    }

    public void release() {
        this.occupied = false;
        this.bookingId = null;
    }

    public String getId() {
        return row + "-" + number;
    }
}