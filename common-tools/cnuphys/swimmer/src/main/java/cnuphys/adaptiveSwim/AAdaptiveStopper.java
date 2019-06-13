package cnuphys.adaptiveSwim;

public abstract class AAdaptiveStopper implements IAdaptiveStopper {

	protected final double _accuracy;
	protected double _s; //current path length meters
	protected final double _sf; //max pathlength meters
	protected final int _dim;   //dimension of our system
	protected double[] _u; //current state vector
	
	//last step size used
	protected double _hLast = Double.NaN;

	//optional trajectory
	protected AdaptiveSwimTrajectory _trajectory;

	public AAdaptiveStopper(double[] u0, final double sf, final double accuracy, AdaptiveSwimTrajectory trajectory) {
		_dim = u0.length;
		_s = 0;
		_sf = sf;
		_accuracy = accuracy;
		_u = new double[_dim];
		copy(u0, _u);
		_trajectory = trajectory;
		
        if (_trajectory != null) {
        	_trajectory.add(_u, 0);
        }

	}
	
	/**
	 * Get the current path length
	 * @return the current path length in meters
	 */
	@Override
	public double getS() {
		return _s;
	}

	/**
	 * Get the current state vector
	 * @return the current state vector
	 */
	@Override
	public double[] getU() {
		return _u;
	}

	/**
	 * Accept a new integration step
	 * @param snew the new value of s in meters
	 * @param unew the new state vector
	 */
	protected void accept(double snew, double[] unew) {
        copy(unew, _u);
        _s = snew;
        
        //add to trajectory?
        if (_trajectory != null) {
        	_trajectory.add(_u, _s);
        }
	}

	/**
	 * Get the max or final value of the path length in meters
	 * @return the max or final value of the path length
	 */
	@Override
	public double getSmax() {
		return _sf;
	}
	
	/**
	 * Convenience method to get the remaining range,
	 * i.e. sMax - s
	 * @return the remaining range in meters
	 */
	@Override
	public double getRemainingRange() {
		return _sf - _s;
	}

	
	/**
	 * Copy a state vector
	 * @param uSrc the source
	 * @param uDest the destination
	 */
	protected void copy(double uSrc[], double[] uDest) {
		System.arraycopy(uSrc, 0, uDest, 0, _dim);
	}



}
