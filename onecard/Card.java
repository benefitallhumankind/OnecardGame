package onecard;

import java.io.Serializable;

public class Card implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1408279119512359101L;
	int number; // A ~ 10,J(11),Q(12),K(13)
	String shape; // Spade, Dia, Clover, Heart

	Card(int number, String shape) {
		this.shape = shape;
		this.number = number;
	}
	Card( String shape ){
		this.shape = shape;
		this.number = -1;
	}
	@Override
	public String toString() {
		return "(" + this.shape + " " + this.number + ")";
	}
}
