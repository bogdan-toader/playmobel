package org.mwg.ml.algorithm.profiling;

import java.util.Random;

class KMeans {

    int[][] getClusterIds(double[][] data, int numOfCluster, int numOfIterations, double[] div) {
        int[][] result = new int[numOfCluster][];
        int features = data[0].length;
        int[] totals = new int[numOfCluster];

        double[][] centroids = new double[numOfCluster][];
        for (int i = 0; i < numOfCluster; i++) {
            centroids[i] = new double[features];
        }

        int[] categories = new int[data.length];

        for (int i = 0; i < numOfCluster; i++) {
            System.arraycopy(data[i], 0, centroids[i], 0, features);
        }

        for (int iter = 0; iter < numOfIterations; iter++) {
            for (int i = 0; i < totals.length; i++) {
                totals[i] = 0;
            }
            //Assign categories
            for (int i = 0; i < data.length; i++) {
                categories[i] = calculateCategory(data[i], centroids, div);
                totals[categories[i]]++;
            }

            //Clear centroids
            for (int i = 0; i < centroids.length; i++) {
                for (int j = 0; j < features; j++) {
                    centroids[i][j] = 0;
                }
            }

            //Add up centroids
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < features; j++) {
                    centroids[categories[i]][j] += data[i][j];
                }
            }

            for (int i = 0; i < centroids.length; i++) {
                if (totals[i] != 0) {
                    for (int j = 0; j < features; j++) {
                        centroids[i][j] = centroids[i][j] / totals[i];
                    }
                } else {
                    Random rand = new Random();
                    double[] avg = data[rand.nextInt(data.length)];
                    System.arraycopy(avg, 0, centroids[i], 0, features);
                }
            }
        }


        //Reshape the result array
        for (int i = 0; i < numOfCluster; i++) {
            result[i] = new int[totals[i]];
            int k = 0;
            for (int j = 0; j < data.length; j++) {
                if (categories[j] == i) {
                    result[i][k] = j;
                    k++;
                }
            }
        }
        return result;
    }

    private int calculateCategory(double[] values, double[][] centroids, double[] div) {

        double min = Double.MAX_VALUE;
        int pos = 0;
        for (int i = 0; i < centroids.length; i++) {
            double d = distance(values, centroids[i], div);
            if (d < min) {
                min = d;
                pos = i;
            }
        }
        return pos;
    }

    static double distance(double[] features, double[] avg, double[] precisions) {
        double max = 0;
        double temp;
        for (int i = 0; i < features.length; i++) {
            temp = (features[i] - avg[i]) * (features[i] - avg[i]) / precisions[i];
            if (temp > max) {
                max = temp;
            }
        }
        return Math.sqrt(max);
    }


}
