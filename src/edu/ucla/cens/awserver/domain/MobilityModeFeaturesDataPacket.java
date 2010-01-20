package edu.ucla.cens.awserver.domain;

/**
 * Domain object representing a mobility mode_features data packet.
 * 
 * @author selsky
 */
public class MobilityModeFeaturesDataPacket extends MobilityModeOnlyDataPacket {
	private Double _speed;
	private Double _variance;
	private Double _average;
	private Double[] _fftArray;
	
	public MobilityModeFeaturesDataPacket() {
		
	}
	
	public Double getSpeed() {
		return _speed;
	}
	public void setSpeed(Double speed) {
		_speed = speed;
	}
	public Double getVariance() {
		return _variance;
	}
	public void setVariance(Double variance) {
		_variance = variance;
	}
	public Double getAverage() {
		return _average;
	}
	public void setAverage(Double average) {
		_average = average;
	}
	public Double[] getFftArray() {
		return _fftArray;
	}
	public void setFftArray(Double[] fftArray) {
		_fftArray = fftArray;
	}
	
}