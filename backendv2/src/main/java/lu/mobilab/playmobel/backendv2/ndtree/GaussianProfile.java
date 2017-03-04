package lu.mobilab.playmobel.backendv2.ndtree;


/**
 * Created by assaad on 21/10/2016.
 */
public class GaussianProfile {
    private double[] _min;
    private double[] _max;
    private double[] _sum;
    private double[] _sumsq;
    private int total;

    public double[] getMin() {
        if (total == 0) {
            return null;
        }
        if (total == 1) {
            return _sum.clone();
        } else {
            return _min.clone();
        }
    }

    public double[] getMax() {
        if (total == 0) {
            return null;
        }
        if (total == 1) {
            return _sum.clone();
        } else {
            return _max.clone();
        }
    }

    public double[] getAvg() {
        if (total == 0) {
            return null;
        }
        if (total == 1) {
            return _sum.clone();
        } else {
            double[] avg = _sum.clone();
            for (int i = 0; i < avg.length; i++) {
                avg[i] = avg[i] / total;
            }
            return avg;
        }
    }



    public void learnNumber(double[] values, int number) {
        if (number == 0) {
            return;
        } else if (number == 1) {
            learn(values);
        } else {
            int features = values.length;

            if (total == 0) {
                _sum = new double[features];
                _min = new double[features];
                _max = new double[features];
                System.arraycopy(values, 0, _min, 0, features);
                System.arraycopy(values, 0, _max, 0, features);
            } else if (total == 1) {
                createMinMax(features);
            }

            //Update the values
            for (int i = 0; i < features; i++) {
                if (values[i] < _min[i]) {
                    _min[i] = values[i];
                }

                if (values[i] > _max[i]) {
                    _max[i] = values[i];
                }
                _sum[i] += values[i] * number;
            }

            int count = 0;
            for (int i = 0; i < features; i++) {
                for (int j = i; j < features; j++) {
                    _sumsq[count] += values[i] * values[j] * number;
                    count++;
                }
            }
            total += number;
        }

    }


    private void createMinMax(int features) {
        _min = new double[features];
        _max = new double[features];
        System.arraycopy(_sum, 0, _min, 0, features);
        System.arraycopy(_sum, 0, _max, 0, features);
        _sumsq = new double[features * (features + 1) / 2];
        int count = 0;
        for (int i = 0; i < features; i++) {
            for (int j = i; j < features; j++) {
                _sumsq[count] = _sum[i] * _sum[j];
                count++;
            }
        }
    }

    public void learn(double[] values) {
        int features = values.length;

        //Create dirac
        if (total == 0) {
            _sum = new double[features];
            System.arraycopy(values, 0, _sum, 0, features);
            total = 1;
        } else {
            //Upgrade dirac to gaussian
            if (total == 1) {
                createMinMax(features);
            }

            //Update the values
            for (int i = 0; i < features; i++) {
                if (values[i] < _min[i]) {
                    _min[i] = values[i];
                }

                if (values[i] > _max[i]) {
                    _max[i] = values[i];
                }
                _sum[i] += values[i];
            }

            int count = 0;
            for (int i = 0; i < features; i++) {
                for (int j = i; j < features; j++) {
                    _sumsq[count] += values[i] * values[j];
                    count++;
                }
            }
            total++;
        }
    }


    public static String printArray(String name, double[] array) {
        String s = name + ": ";
        for (int i = 0; i < array.length; i++) {
            s += array[i];
            if (i < array.length - 1) {
                s += ", ";
            }
        }
        return s;
    }

    public void print() {
        System.out.println("Statistics:");
        System.out.println("Total: " + total);
        System.out.println(printArray("min", getMin()));
        System.out.println(printArray("max", getMax()));
        System.out.println(printArray("avg", getAvg()));
        System.out.println("");
    }


    public static double[] predict(double[] output, double prediction, double err) {
        double[] result = new double[output.length];
        double total = 0;

        double d = 1 / Math.sqrt(2 * err * err * Math.PI);
        for (int i = 0; i < output.length; i++) {
            result[i] = d * Math.exp(-((output[i] - prediction) * (output[i] - prediction)) / (2 * err * err));
            total += result[i];
        }

        for (int i = 0; i < output.length; i++) {
            result[i] = result[i] / total;
        }

        return result;
    }
}
