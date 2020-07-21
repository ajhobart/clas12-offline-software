package cnuphys.bCNU.magneticfield.swim;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Vector;

import cnuphys.bCNU.drawable.DrawableAdapter;
import cnuphys.bCNU.graphics.GraphicsUtilities;
import cnuphys.bCNU.graphics.SymbolDraw;
import cnuphys.bCNU.graphics.container.IContainer;
import cnuphys.bCNU.graphics.style.LineStyle;
import cnuphys.lund.LundId;
import cnuphys.lund.LundStyle;
import cnuphys.splot.plot.X11Colors;
import cnuphys.swim.IProjector;
import cnuphys.swim.SwimMenu;
import cnuphys.swim.SwimTrajectory;
import cnuphys.swim.SwimTrajectory2D;
import cnuphys.swim.Swimming;

/**
 * An abstract class for dwaing trajectories from thje swimmer
 * 
 * @author heddle
 *
 */
public abstract class ASwimTrajectoryDrawer extends DrawableAdapter implements IProjector {

	// colors
	protected static final Color sectChangeColor = X11Colors.getX11Color("purple", 128);
	protected static final Color tracerColor = new Color(0, 0, 0, 50);
	protected static final Stroke tracerStroke = GraphicsUtilities.getStroke(3f, LineStyle.SOLID);
	protected static final Stroke planeStroke = GraphicsUtilities.getStroke(1.5f, LineStyle.SOLID);

	private static RenderingHints renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
	static {
		renderHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	};

	// these are the 2D trajectories
	protected Vector<SwimTrajectory2D> _trajectories2D = new Vector<SwimTrajectory2D>(20);

	protected SwimTrajectory2D _closestTrajectory;

	protected double _minMarkR = 25; // cm

	/**
	 * Actual drawing method
	 * 
	 * @param g         the graphics context
	 * @param container the base container
	 */
	@Override
	public void draw(Graphics g, IContainer container) {
		// draw any trajectories generated by swimming. These are not in
		// the event

		_trajectories2D.clear();
		_closestTrajectory = null;

		if (SwimMenu.getInstance().showMonteCarloTracks()) {
			List<SwimTrajectory> trajectories = Swimming.getMCTrajectories();
			
			if (trajectories != null) {

				for (SwimTrajectory trajectory : trajectories) {
					// give a chance to veto a trajectory, e.g. no chance it
					// will
					// appear on this view (for example)
					if (!veto(trajectory)) {
						_trajectories2D.add(new SwimTrajectory2D(trajectory, this));
					}
				}
				
				drawTrajectories(g, container);
			}
		}

		// reconstructed?
		if (SwimMenu.getInstance().showReconstructedTracks()) {
			List<SwimTrajectory> trajectories = Swimming.getReconTrajectories();

			if (trajectories != null) {

				for (SwimTrajectory trajectory : trajectories) {

					// give a chance to veto a trajectory, e.g. no chance it
					// will
					// appear on this view (for example)
					if (!veto(trajectory)) {
						_trajectories2D.add(new SwimTrajectory2D(trajectory, this));
					}
				}

				drawTrajectories(g, container);
			}
		}
		
		//aux tracks
		if (true) {
			List<SwimTrajectory> trajectories = Swimming.getAuxTrajectories();

			if (trajectories != null) {

				for (SwimTrajectory trajectory : trajectories) {
					if (!veto(trajectory)) {
						_trajectories2D.add(new SwimTrajectory2D(trajectory, this));
					}
				}

				drawTrajectories(g, container);
			}
		}

		
	}
	
	protected void drawTrajectories(Graphics g, IContainer container) {
		for (SwimTrajectory2D trajectory2D : _trajectories2D) {
			drawSwimTrajectory(g, container, trajectory2D);
		}
	}

	/**
	 * Here we have a chance to veto a trajectory. For example, we may decide that
	 * the trajectory won't appear on this view (assuming a view owns this drawer)
	 * and so don't bother to compute it. The default implementation vetoes nothing.
	 * 
	 * @param trajectory the trajectory to test.
	 * @return <code>true</code> if this trajectory is vetoed.
	 */
	protected boolean veto(SwimTrajectory trajectory) {
		return false;
	}

	// denote sector changes
	protected void markSectorChanges(Graphics g, IContainer container, SwimTrajectory2D trajectory) {

		int[] indices = trajectory.sectChangeIndices();
		if (indices == null) {
			return;
		}

		SwimTrajectory traj3D = trajectory.getTrajectory3D();
		Point pp = new Point();
		for (int idx : indices) {

			double r = traj3D.getR(idx);
			if (r > _minMarkR) {
				Point2D.Double wp = trajectory.getPath()[idx];
				container.worldToLocal(pp, wp);
				SymbolDraw.drawDiamond(g, pp.x, pp.y, 5, Color.cyan, sectChangeColor);
			}
		}
	}

	/**
	 * Draw a trajectory
	 * 
	 * @param g          the graphics object
	 * @param container  the rendering container
	 * @param trajectory the 2D (already projected) trajectory to draw
	 */
	protected void drawSwimTrajectory(Graphics g, IContainer container, SwimTrajectory2D trajectory) {

		if (!acceptSimpleTrack(trajectory)) {
			return;
		}

		LundId lid = trajectory.getTrajectory3D().getLundId();

		String source = trajectory.getSource().toLowerCase();

		if (source.contains("hitbasedtrkg::hbtracks")) {
			plainDrawSwimTrajectory(g, container, trajectory, Color.yellow);
			return;
		} else if (source.contains("timebasedtrkg::tbtracks")) {
			plainDrawSwimTrajectory(g, container, trajectory, X11Colors.getX11Color("dark orange"));
			return;
		} else if (source.contains("cvtrec::tracksca")) {
			plainDrawSwimTrajectory(g, container, trajectory, X11Colors.getX11Color("red"));
			return;
		} else if (source.contains("cvtrec::tracks")) {
			plainDrawSwimTrajectory(g, container, trajectory, X11Colors.getX11Color("dark green"));
			return;
		} else if (source.contains("hitbasedtrkg::aitracks")) {
			plainDrawSwimTrajectory(g, container, trajectory, X11Colors.getX11Color("spring green"));
			return;
		} else if (source.contains("timebasedtrkg::aitracks")) {
			plainDrawSwimTrajectory(g, container, trajectory, X11Colors.getX11Color("magenta"));
			return;
		}

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHints(renderHints);

		Stroke oldStroke = g2.getStroke();

		Polygon poly = new Polygon();
		Point2D.Double path[] = trajectory.getPath();

		if (path == null) {
			System.err.println("Null path");
			return;
		}

		Point pp = new Point();

		for (Point2D.Double wp : path) {
			container.worldToLocal(pp, wp);
			poly.addPoint(pp.x, pp.y);
		}

		if (poly.npoints > 1) {

			// tracer traj
			g2.setColor(tracerColor);
			g2.setStroke(tracerStroke);
			g.drawPolyline(poly.xpoints, poly.ypoints, poly.npoints);

			LundStyle style = LundStyle.getStyle(lid);
			g.setColor(style.getLineColor());
			g2.setStroke(style.getStroke());
			// g.drawPolyline(poly.xpoints, poly.ypoints, poly.npoints);

			GraphicsUtilities.drawHighlightedPolyline(g2, poly.xpoints, poly.ypoints, poly.npoints,
					style.getLineColor(), getHighlightColor(lid));

		}

		g2.setStroke(oldStroke);
	}

	public abstract boolean acceptSimpleTrack(SwimTrajectory2D trajectory);

	/**
	 * Draw a trajectory
	 * 
	 * @param g          the graphics object
	 * @param container  the rendering container
	 * @param trajectory the 2D (already projected) trajectory to draw
	 */
	private void plainDrawSwimTrajectory(Graphics g, IContainer container, SwimTrajectory2D trajectory, Color color) {

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHints(renderHints);

		Stroke oldStroke = g2.getStroke();

		Polygon poly = new Polygon();
		Point2D.Double path[] = trajectory.getPath();

		if (path == null) {
			System.err.println("Null path");
			return;
		}

		Point pp = new Point();

		for (Point2D.Double wp : path) {
			container.worldToLocal(pp, wp);
			poly.addPoint(pp.x, pp.y);
		}

		if (poly.npoints > 1) {
			g2.setColor(color);
			g2.setStroke(planeStroke);
			g.drawPolyline(poly.xpoints, poly.ypoints, poly.npoints);
		}

		g2.setStroke(oldStroke);
	}

	/**
	 * Obtain the highlight color for drawing a particle trajectory
	 * 
	 * @param lid the Lund Id
	 * @returnthe highlight color for drawing a particle trajectory
	 */
	public static Color getHighlightColor(LundId lid) {
		Color hcolor = null;
		if (lid != null) {
			String type = lid.getType();

			boolean particle = lid.getId() >= 0;

			if ("Lepton".equalsIgnoreCase(type)) {
				hcolor = particle ? Color.red : Color.cyan;
			} else if ("Meson".equalsIgnoreCase(type)) {
				hcolor = particle ? Color.green : Color.magenta;
			} else if ("Baryon".equalsIgnoreCase(type)) {
				hcolor = particle ? Color.blue : Color.yellow;
			} else if ("Interboson".equalsIgnoreCase(type)) {
				hcolor = Color.lightGray;
			} else {
				hcolor = Color.lightGray;
			}
		}
		return hcolor;
	}

	/**
	 * Get the distance of closest approach to any 2D (projected) trajectory.
	 * 
	 * @param wp the point in question
	 * @return the closest distance. The closest trajectory will be cached in
	 *         <code>closestTrajectory</code>.
	 */
	public double closestApproach(Point2D.Double wp) {
		_closestTrajectory = null;

		double minDist = Double.POSITIVE_INFINITY;
		if ((_trajectories2D == null) || (_trajectories2D.size() < 1)) {
			return minDist;
		}

		// loop over all trajectories
		for (SwimTrajectory2D trajectory2D : _trajectories2D) {
			double dist = trajectory2D.closestDistance(wp);

			if (dist < minDist) {
				_closestTrajectory = trajectory2D;
				minDist = dist;
			}
		}

		return minDist;
	}

	/**
	 * @return the closestTrajectory
	 */
	public SwimTrajectory2D getClosestTrajectory() {
		return _closestTrajectory;
	}

}
