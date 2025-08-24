package dev.xqedii.xqediiclient.client.toast;

public class Toast {
    private final String title;
    private final String message;
    private final long startTime;
    private final long duration;

    public static final long ANIM_DURATION = 400;

    public Toast(String title, String message, long duration) {
        this.title = title;
        this.message = message;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public long getDuration() { return duration; } // DODANA METODA

    public long getAge() {
        return System.currentTimeMillis() - startTime;
    }

    public boolean isExpired() {
        return getAge() > duration + (ANIM_DURATION * 2);
    }
}