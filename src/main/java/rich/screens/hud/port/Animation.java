package rich.screens.hud.port;

public class Animation {

    private final FloatAnim anim;

    public Animation(long duration, Easing easing) {
        this.anim = new FloatAnim(duration, easing);
    }

    public Animation(long duration, float start, Easing easing) {
        this.anim = new FloatAnim(duration, start, easing);
    }

    public void update(float target) {
        anim.update(target);
    }

    public float getValue() {
        return anim.getValue();
    }

    public void setValue(float value) {
        anim.setValue(value);
    }

    public void setStartValue(float value) {
        anim.setStartValue(value);
    }

    public void setDuration(long duration) {
        anim.setDuration(duration);
    }

    public void setEasing(Easing easing) {
        anim.setEasing(easing);
    }
}
