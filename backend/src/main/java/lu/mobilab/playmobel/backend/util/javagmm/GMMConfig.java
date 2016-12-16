package lu.mobilab.playmobel.backend.util.javagmm;

/**
 * Created by assaa on 15/12/2016.
 */
public class GMMConfig {
    public int maxLevels;
    public int width;
    public int compressionFactor;
    public int compressionIter;
    public double threshold;
    public double[] resolution;

    public GMMConfig() {

    }


    public GMMConfig(int maxLevels, int width, int compressionFactor, int compressionIter, double threshold, double[] resolution) {
        this.maxLevels=maxLevels;
        this.width=width;
        this.compressionFactor=compressionFactor;
        this.compressionIter=compressionIter;
        this.threshold=threshold;
        this.resolution=resolution;
    }

}
