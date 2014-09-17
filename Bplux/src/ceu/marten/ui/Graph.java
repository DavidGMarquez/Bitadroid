package ceu.marten.ui;

import java.io.Serializable;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint.Align;
import ceu.marten.bitadroid.R;

import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;

/**
 * Class representing a lineGraphView graph with initialized serie and custom title
 * @author Carlos Marten
 *
 */
public class Graph implements Serializable {

	private static final long serialVersionUID = -5122704369223869018L;

	private GraphViewSeries basicSerie;
	private GraphViewSeriesStyle style;
	private long xValue;
	private GraphView graphView;

	@SuppressWarnings("deprecation")
	public Graph(android.content.Context context, String title) {
		// SET THE SERIE STYLE
		style = new GraphViewSeriesStyle(getChannelColor(
				Integer.parseInt(title.charAt(title.length() - 1) + ""),
				context), 2); // 2 -> thickness

		// INIT SERIE DATA
		basicSerie = new GraphViewSeries(title, style, new GraphViewData[] {
				new GraphViewData(0.5, 125), new GraphViewData(1, 125) });
		// INIT GRAPHVIEW
		graphView = new LineGraphView(context, ""); 

		// ADD SERIES TO GRAPHVIEW and SET SCROLLABLE
		graphView.addSeries(basicSerie);
		graphView.setViewPort(
				2,
				Double.parseDouble(context.getResources().getString(
						R.string.graph_viewport_size)));
		graphView.setScalable(true);
		graphView.setCustomLabelFormatter(new CustomLabelFormatter() {
			@SuppressLint("DefaultLocale")
			@Override
			public String formatLabel(long value, boolean isValueX) {
				if (isValueX) {
					if (value < 0.000){
						xValue = 0;
						return "00:00:00";
					}
					xValue = value;
					return String.format("%02d:%02d:%02d",(int) ((value / (1000*60*60)) % 24), (int) ((value / (1000*60)) % 60), (int) (value / 1000) % 60);
				} else
					return String.valueOf((int)(value)); // vertical labels value
			}
		});

		// SET THE GRAPH VIEW STYLE
		GraphViewStyle graphStyle = new GraphViewStyle();
		graphStyle.setNumHorizontalLabels(Integer.parseInt(context
				.getResources().getString(
						R.string.graph_numberof_horizontal_labels)));
		graphStyle.setVerticalLabelsAlign(Align.LEFT);
		graphStyle.setGridColor(context.getResources().getColor(
				R.color.light_grey));
		graphStyle.setHorizontalLabelsColor(context.getResources().getColor(
				R.color.grey));
		graphStyle.setVerticalLabelsColor(context.getResources().getColor(
				R.color.grey));
		graphStyle.setTextSize(Float.parseFloat(context.getResources()
				.getString(R.string.graph_labels_text_size)));
		graphStyle
				.setVerticalLabelsWidth(Integer.parseInt((context
						.getResources()
						.getString(R.string.graph_vertical_labels_width))));

		graphView.setGraphViewStyle(graphStyle);

		// SET THE LEGEND
		graphView.setLegendWidth((Integer.parseInt((context.getResources()
				.getString(R.string.graph_legend_width)))));
		graphView.setLegendAlign(LegendAlign.BOTTOM);
		graphView.setShowLegend(true);
	}

	/**
	 * Each of the eight channels of the bioplux device have a fix color.
	 * Returns the color of the respective channel
	 * 
	 * @param channelNumber 
	 * 			  number of channels to display
	 * @param context
	 *            used to get color resources
	 * @return integer
	 */
	private int getChannelColor(int channelNumber, Context context) {
		switch (channelNumber) {
		default:
			return context.getResources().getColor(R.color.default_channel);
		case 2:
			return context.getResources().getColor(R.color.channel2);
		case 3:
			return context.getResources().getColor(R.color.channel3);
		case 4:
			return context.getResources().getColor(R.color.channel4);
		case 5:
			return context.getResources().getColor(R.color.channel5);
		case 6:
			return context.getResources().getColor(R.color.channel6);
		case 7:
			return context.getResources().getColor(R.color.channel7);
		case 8:
			return context.getResources().getColor(R.color.channel8);
		}
	}

	public GraphView getGraphView() {
		return graphView;
	}

	public GraphViewSeries getSerie() {
		return basicSerie;
	}

	public void setSerie(GraphViewSeries serie) {
		this.basicSerie = serie;
	}

	public long getxValue() {
		return xValue;
	}

	public void setxValue(long xValue) {
		this.xValue = xValue;
	}

}
