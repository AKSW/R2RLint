package org.aksw.sparqlify.qa.metrics.semanticaccuracy;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.aksw.commons.util.MapReader;
import org.aksw.sparqlify.core.domain.input.ViewDefinition;
import org.aksw.sparqlify.qa.exceptions.NotImplementedException;
import org.aksw.sparqlify.qa.metrics.semanticaccuracy.PreservedFkeyConstraint;
import org.aksw.sparqlify.qa.sinks.BooleanTestingSink;
import org.aksw.sparqlify.util.SparqlifyUtils;
import org.aksw.sparqlify.util.ViewDefinitionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:test_bool_beans.xml"})
public class PreservedFkeyConstraintTest {

	@Autowired
	private BooleanTestingSink sink;
	@Autowired
	private DataSource rdb;
	private Connection conn;
	private ViewDefinitionFactory vdf;
	
	@Autowired
	private PreservedFkeyConstraint metric;
	
	
	@PostConstruct
	private void init() throws SQLException {
		conn = rdb.getConnection();
	}
	
	@PreDestroy
	private void cleanUp() throws SQLException {
		conn.close();
	}
	
	@Before
	public void setUp() throws Exception {
		initDBContent();
		Map<String, String> typeAlias = MapReader.read(
				new File("src/test/resources/type-map.h2.tsv"));
		vdf = SparqlifyUtils.createDummyViewDefinitionFactory(typeAlias);
	}

	@After
	public void tearDown() throws Exception {
		conn.createStatement().executeUpdate("DROP TABLE IF EXISTS test1;");
		conn.createStatement().executeUpdate("DROP TABLE IF EXISTS test2;");
	}
	
	private void initDBContent() throws SQLException {
		conn.createStatement().executeUpdate("DROP TABLE IF EXISTS test1;");
		conn.createStatement().executeUpdate("DROP TABLE IF EXISTS test2;");
		
		conn.createStatement().executeUpdate("CREATE TABLE test1 ("
					+ "id integer NOT NULL PRIMARY KEY, "
					+ "default_name varchar(400), "
					+ "name varchar(400) "
				+ ");");
		
		conn.createStatement().executeUpdate("CREATE TABLE test2 ("
					+ "id integer NOT NULL PRIMARY KEY, "
					+ "name varchar(400), "
					+ "test1_id integer REFERENCES test1(id)"
				+ ");");
		conn.commit();
	}
	
	private ViewDefinition viewDef01() throws IOException {
		return vdf.create(
				"Prefix ex: <http://ex.org/> " +
				"Prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"Prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
				"Prefix foaf:<http://xmlns.com/foaf/0.1/> " +
				"Prefix xsd:<http://www.w3.org/2001/XMLSchema#> " +
				"Prefix owl:<http://www.w3.org/2002/07/owl#> " +
				"Prefix spy:<http://aksw.org/sparqlify/> " +
				"Create View view01 As " +
				"Construct { " +
					"?r2 ex:prop1 ?name . " +
					"?r1 a ex:Sth . " +
				"} " +
				"With " +
					"?r2 = uri(ex:resource, '/', ?ID) " +
					"?name = plainLiteral(?NAME) " +
					"?r1 = uri(ex:resource, '/', ?TEST1_ID) " +
				"From " +
					"TEST2");
	}
	
	@Test
	public synchronized void test01() throws NotImplementedException,
			SQLException, IOException {
		
		String metricName = "test01";
		metric.setName(metricName);
		metric.setParentDimension("parent");
		metric.initMeasureDataSink();
		
		metric.assessViews(Arrays.asList(viewDef01()));
		
		assertTrue(sink.measureWritten(metricName));
	}
	
	
	private ViewDefinition viewDef02() throws IOException {
		return vdf.create(
				"Prefix ex: <http://ex.org/> " +
				"Prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"Prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
				"Prefix foaf:<http://xmlns.com/foaf/0.1/> " +
				"Prefix xsd:<http://www.w3.org/2001/XMLSchema#> " +
				"Prefix owl:<http://www.w3.org/2002/07/owl#> " +
				"Prefix spy:<http://aksw.org/sparqlify/> " +
				"Create View view02 As " +
				"Construct { " +
					"?r2 ex:prop1 ?name . " +
					"?r1 a ex:Sth . " +
					"?r1 ex:prop2 ?r2 . " +
				"} " +
				"With " +
					"?r2 = uri(ex:resource, '/', ?ID) " +
					"?name = plainLiteral(?NAME) " +
					"?r1 = uri(ex:resource, '/', ?TEST1_ID) " +
				"From " +
					"TEST2");
	}
	
	@Test
	public synchronized void test02() throws NotImplementedException,
			SQLException, IOException {
		
		String metricName = "test02";
		metric.setName(metricName);
		metric.setParentDimension("parent");
		metric.initMeasureDataSink();
		
		metric.assessViews(Arrays.asList(viewDef02()));
		
		assertTrue(sink.measureWritten(metricName));
	}
	
	
	private ViewDefinition viewDef03() throws IOException {
		return vdf.create(
				"Prefix ex: <http://ex.org/> " +
				"Prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"Prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
				"Prefix foaf:<http://xmlns.com/foaf/0.1/> " +
				"Prefix xsd:<http://www.w3.org/2001/XMLSchema#> " +
				"Prefix owl:<http://www.w3.org/2002/07/owl#> " +
				"Prefix spy:<http://aksw.org/sparqlify/> " +
				"Create View view03 As " +
				"Construct { " +
					"?r2 ex:prop1 ?name . " +
					"?r1 a ex:Sth . " +
					"?r2 ex:prop2 ?r1 . " +
				"} " +
				"With " +
					"?r2 = uri(ex:resource, '/', ?ID) " +
					"?name = plainLiteral(?NAME) " +
					"?r1 = uri(ex:resource, '/', ?TEST1_ID) " +
				"From " +
					"TEST2");
	}
	
	@Test
	public synchronized void test03() throws NotImplementedException,
			SQLException, IOException {
		
		String metricName = "test03";
		metric.setName(metricName);
		metric.setParentDimension("parent");
		metric.initMeasureDataSink();
		
		metric.assessViews(Arrays.asList(viewDef03()));
		
		assertFalse(sink.measureWritten(metricName));
	}
}
