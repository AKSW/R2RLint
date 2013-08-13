package org.aksw.sparqlify.qa.sinks;

import java.util.Set;

import org.aksw.sparqlify.core.algorithms.ViewQuad;
import org.aksw.sparqlify.core.domain.input.ViewDefinition;

import com.hp.hpl.jena.graph.Triple;

public class TripleMeasureDatum extends MeasureDatum {

	Triple triple;

	public TripleMeasureDatum(String dimension, String metric, float value, Triple triple,
			Set<ViewQuad<ViewDefinition>> viewQuads) {
		
		this.dimension = dimension;
		this.metric = metric;
		this.value = value;
		this.triple = triple;
		this.viewQuads = viewQuads;
		
	}


	public Triple getTriple() {
		return triple;
	}
}
