package ro.cristian.opentelemetry_example;

import java.util.concurrent.ThreadLocalRandom;

public class Dice {
  public int roll(int min, int max) {
    return ThreadLocalRandom.current().nextInt(min, max + 1);
  }
}