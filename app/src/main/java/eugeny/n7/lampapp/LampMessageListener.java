package eugeny.n7.lampapp;

public interface LampMessageListener {
    public void colorReceived(int red, int green, int blue);

    public void stateReceived(int brightness, int speed, int hold);
}
