package rich.screens.hud.port;

public class FloatAnim {
    private long durationMs = 200;
    private float value;
    private long last = -1;

    public FloatAnim(long durationMs) {
        this.durationMs = durationMs;
    }

    public FloatAnim(long durationMs, Easing easing) {
        this.durationMs = durationMs;
    }

    public FloatAnim(long durationMs, float initialValue, Easing easing) {
        this.durationMs = durationMs;
        this.value = initialValue;
    }

    public void update(float target) {
        long now = System.currentTimeMillis();
        if (last == -1) {
            value = target;
            last = now;
            return;
        }
        float dt = Math.max(0.0f, now - last);
        last = now;
        float k = (float) (1.0 - Math.exp(-dt / (durationMs * 0.4)));
        value += (target - value) * k;
        if (Math.abs(target - value) < 0.0005f) value = target;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public void setStartValue(float value) {
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    public void setDuration(long durationMs) {
        this.durationMs = durationMs;
    }

    public void setEasing(Easing easing) {
    }
}
