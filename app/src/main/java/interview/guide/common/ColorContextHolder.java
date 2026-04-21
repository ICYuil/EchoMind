package interview.guide.common;

public class ColorContextHolder {
    private static final ThreadLocal<String> COLOR_ID_HOLDER = new ThreadLocal<>();

    public static void setColorId(String colorId) {
        COLOR_ID_HOLDER.set(colorId);
    }

    public static String getColorId() {
        return COLOR_ID_HOLDER.get();
    }

    public static void clear() {
        COLOR_ID_HOLDER.remove();
    }
}