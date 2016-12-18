package lu.mobilab.playmobel.backendv2.ndtree;

/**
 * Created by assaa on 17/12/2016.
 */
public class NDTreeConfig {
    //config
    private double[] _resolution;
    private int _maxPerLevel;
    private double[] _min;
    private double[] _max;


    private GaussianProfile profile;
    private int maxlev = 0;
    private long jumpcounter = 0;
    private int createdNodes=0;


    public void updateMaxLev(int lev){
        if (lev>maxlev){
            maxlev=lev;
        }
    }



    public void incJumpCounter(){
        jumpcounter++;
    }

    public void incCreatedNode(){
        createdNodes++;
    }

    public int getMaxlev() {
        return maxlev;
    }

    public long getJumpcounter() {
        return jumpcounter;
    }

    public int getCreatedNodes() {
        return createdNodes;
    }

    public void updateStat(double[] values){
        profile.learn(values);
    }

    public GaussianProfile getProfile(){
        return profile;
    }

    public double[] getResolution(){
        return _resolution;
    }

    public int getMaxPerLevel(){
        return _maxPerLevel;
    }

    public double[] getMin() {
        return _min;
    }

    public double[] getMax() {
        return _max;
    }

    public NDTreeConfig(double[] min, double[] max, double[] resolution, int maxPerLevel){
        if (min.length != max.length || min.length != resolution.length) {
            throw new RuntimeException("min, max, resolution should have the same dimension");
        }
        if (min.length > 16) {
            throw new RuntimeException("Number of dimensions are too big to handle!");
        }

        for (int i = 0; i < min.length; i++) {
            if (min[i] >= max[i]) {
                throw new RuntimeException("Min should always be < max");
            }
            if (resolution[i] <= 0) {
                throw new RuntimeException("Resolution should always be > 0");
            }
        }
        this._min = min;
        this._max = max;
        this._resolution = resolution;
        this._maxPerLevel = maxPerLevel;
        this.profile=new GaussianProfile();
    }


}
