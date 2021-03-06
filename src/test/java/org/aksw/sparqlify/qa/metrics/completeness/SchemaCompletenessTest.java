package org.aksw.sparqlify.qa.metrics.completeness;

import static org.junit.Assert.assertEquals;

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
import org.aksw.sparqlify.qa.sinks.MeasureDataSink;
import org.aksw.sparqlify.qa.sinks.ValueTestingSink;
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
@ContextConfiguration(locations={"classpath:test_val_beans.xml"})
public class SchemaCompletenessTest {

	@Autowired
	private DataSource rdb;
	private Connection conn;
	@Autowired
	private SchemaCompleteness metric;
	@Autowired
	private MeasureDataSink sink;
	
	private ViewDefinition employeeView1;
	private ViewDefinition employeeView2;
	private ViewDefinition deptView;

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
		initDBContent(conn);
		initViewDefinitions();
	}
	
	@After
	public void tearDown() throws SQLException {
		conn.createStatement().executeUpdate("DROP TABLE IF EXISTS dept;");
		conn.createStatement().executeUpdate("DROP TABLE IF EXISTS dept_translation");
		conn.createStatement().executeUpdate("DROP TABLE IF EXISTS employee");
	}
	
	private void initDBContent(Connection conn) throws SQLException {
		
		conn.createStatement().executeUpdate("DROP TABLE IF EXISTS dept;");
		conn.createStatement().executeUpdate("DROP TABLE IF EXISTS dept_translation");
		conn.createStatement().executeUpdate("DROP TABLE IF EXISTS employee");
		
		conn.createStatement().executeUpdate("CREATE TABLE dept (" +
				"id integer NOT NULL, " +
				"default_name character varying(400) NOT NULL );");
		
		conn.createStatement().executeUpdate("CREATE TABLE dept_translation (" +
				"id integer NOT NULL, " +
				"lang character varying(3) NOT NULL, " +
				"name character varying(400) NOT NULL);");
		
		conn.createStatement().executeUpdate("CREATE TABLE employee (" +
				"id integer NOT NULL, " +
				"firstname character varying(40), " +
				"lastname character varying(60) NOT NULL, " +
				"employment_time integer, " +
				"employment_time_unit character varying(20), " +
				"birthday date NOT NULL, " +
				"salary_monthly integer NOT NULL, " +
				"web_id character varying(355), " +
				"married boolean, dept integer);");

		conn.createStatement().executeUpdate("INSERT INTO dept VALUES(1, 'it');");
		conn.createStatement().executeUpdate("INSERT INTO dept VALUES(2, 'accounting');");
		conn.createStatement().executeUpdate("INSERT INTO dept VALUES(3, 'production');");
		conn.createStatement().executeUpdate("INSERT INTO dept VALUES(4, 'marketing');");

		conn.createStatement().executeUpdate("INSERT INTO dept_translation VALUES(1, 'en', 'IT');");
		conn.createStatement().executeUpdate("INSERT INTO dept_translation VALUES(1, 'de', 'IT');");
		conn.createStatement().executeUpdate("INSERT INTO dept_translation VALUES(2, 'en', 'Accounting');");
		conn.createStatement().executeUpdate("INSERT INTO dept_translation VALUES(2, 'de', 'Buchhaltung');");
		conn.createStatement().executeUpdate("INSERT INTO dept_translation VALUES(3, 'en', 'Production');");
		conn.createStatement().executeUpdate("INSERT INTO dept_translation VALUES(3, 'de', 'Produktion');");
		conn.createStatement().executeUpdate("INSERT INTO dept_translation VALUES(4, 'en', 'Marketing');");
		conn.createStatement().executeUpdate("INSERT INTO dept_translation VALUES(4, 'de', 'Marketing');");
		conn.createStatement().executeUpdate("INSERT INTO dept_translation VALUES(1, 'sx', 'Ei Dieh');");
		
		conn.createStatement().executeUpdate("INSERT INTO employee VALUES(" +
				"1, 'Alf', 'Alliteration', 3, 'month', '1979-01-05', 3600, " +
				"'http://alf.alliteration.name', true, 1);");
		conn.createStatement().executeUpdate("INSERT INTO employee VALUES(" +
				"2, 'Bela', 'B.', 15, 'year', '1968-02-28', 4200, " +
				"NULL, false, 4);");
		conn.createStatement().executeUpdate("INSERT INTO employee VALUES(" +
				"3, 'Charly', 'Chaplin', 2, 'gYear', '1983-09-17', 1875, " +
				"NULL, false, 4);");
		conn.createStatement().executeUpdate("INSERT INTO employee VALUES(" +
				"4, 'Daniel', 'Defoe', 6, 'month', '1990-12-04', 2200, " +
				"'http://defoe.name/daniel', false, 3);");
		conn.createStatement().executeUpdate("INSERT INTO employee VALUES(" +
				"5, 'Edna', 'Erdmann', 6, 'gYear', '1960-03-03', 2300, " +
				"NULL, true, 2);");
		conn.createStatement().executeUpdate("INSERT INTO employee VALUES(" +
				"6, 'Fred', 'Flintstone', 5, 'gYear', '0975-02-20', 3000," +
				"'http://flintstones.name/fred', true, 1);");
		conn.createStatement().executeUpdate("INSERT INTO employee VALUES(" +
				"7, 'Guiseppe', 'Guarino', 2, 'gYear', '1980-12-14', 3200," +
				"NULL, true, 2);");
		conn.createStatement().executeUpdate("INSERT INTO employee VALUES(" +
				"8, 'Hillary', 'Hart', 4, 'month', '1987-01-23', 2600," +
				"'http://hillary.hart.name', false, 3);");
		conn.createStatement().executeUpdate("INSERT INTO employee VALUES(" +
				"9, 'Iadh', 'Idehen', 3, 'gYear', '1985-04-12', 2200, " +
				"NULL, true, 1);");
		conn.createStatement().executeUpdate("INSERT INTO employee VALUES(" +
				"10, 'Jiao', 'Jin', 11, 'month', '1979-08-07', 2600," +
				"'http://jin.name/ids/jiao', false, 1);");
		conn.createStatement().executeUpdate("INSERT INTO employee VALUES(" +
				"11, 'Hèléna', 'Höllmøllâr', 3, 'gYear', '1977-11-01', 3200, " +
				"NULL, true, 4);");

		conn.createStatement().executeUpdate("ALTER TABLE dept " +
				"ADD CONSTRAINT dept_pkey PRIMARY KEY (id);");

		conn.createStatement().executeUpdate("ALTER TABLE dept_translation " +
				"ADD CONSTRAINT dept_translations_pkey PRIMARY KEY (id, lang, name);");

		conn.createStatement().executeUpdate("ALTER TABLE employee " +
				"ADD CONSTRAINT employees_pkey PRIMARY KEY (id);");
		
		conn.createStatement().executeUpdate("ALTER TABLE dept_translation " +
				"ADD CONSTRAINT dept_translations_id_fkey FOREIGN KEY (id) REFERENCES dept(id);");
		
		conn.createStatement().executeUpdate("ALTER TABLE employee " +
				"ADD CONSTRAINT employees_dept_fkey FOREIGN KEY (dept) REFERENCES dept(id);");
	}

	private void initViewDefinitions() throws IOException {
		Map<String, String> typeAlias = MapReader.read(
				new File("src/test/resources/type-map.h2.tsv"));
		ViewDefinitionFactory vdf =
				SparqlifyUtils.createDummyViewDefinitionFactory(typeAlias);
		
		employeeView1 = vdf.create(
				"Prefix ex: <http://ex.org/> " +
				"Prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"Prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
				"Prefix foaf:<http://xmlns.com/foaf/0.1/> " +
				"Prefix xsd:<http://www.w3.org/2001/XMLSchema#> " +
				"Prefix owl:<http://www.w3.org/2002/07/owl#> " +
				"Prefix spy:<http://aksw.org/sparqlify/> " +
				"Create View employees As " +
				"Construct { " +
					"?empl rdfs:label ?fnme. " +
					"?empl ex:employmentTime ?empt. " +
					"?empl ex:birthDay ?bday. " +
					"?empl ex:salary ?salr. " +
					"?empl ex:webId ?wbid. " +
					"?empl ex:privateWebId ?pwid. " +
					"?empl ex:marriedTo ?spou. " +
					"?empl ex:department ?dept. " +
				"} " +
				"With " +
					"?empl = uri(ex:employee, '/', spy:urlEncode(?lastname), '/', spy:urlEncode(?firstname)) " +
					"?fnme = plainLiteral(concat(?firstname, ' ', ?lastname)) " +
					"?empt = typedLiteral(?employment_time, concat('http://www.w3.org/2001/XMLSchema#', ?employment_time_unit)) " +
					"?bday = typedLiteral(?birthday, xsd:date) " +
					"?salr = typedLiteral(" +
						"?salary_monthly + ?salary_monthly + ?salary_monthly " +
						"+ ?salary_monthly + ?salary_monthly + ?salary_monthly " +
						"+ ?salary_monthly + ?salary_monthly + ?salary_monthly " +
						"+ ?salary_monthly + ?salary_monthly + ?salary_monthly, xsd:int) " +
					"?wbid = uri('http://ex.org/', ?lastname, '/', ?firstname) " +
					"?pwid = uri(?web_id) " +
					"?spou = bNode(?id) " +
					"?dept = uri('http://ex.org/', ?id) " +
				"From " +
					"employee");
		
		employeeView2 = vdf.create(
				"Prefix ex: <http://ex.org/> " +
				"Prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"Prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
				"Prefix foaf:<http://xmlns.com/foaf/0.1/> " +
				"Prefix xsd:<http://www.w3.org/2001/XMLSchema#> " +
				"Prefix owl:<http://www.w3.org/2002/07/owl#> " +
				"Prefix spy:<http://aksw.org/sparqlify/> " +
				"Create View employees As " +
				"Construct { " +
					"?empl rdfs:label ?fnme. " +
					"?empl ?wbid ?empl. " +
					"?empl ex:employmentTime ?empt. " +
					"?empl ex:birthDay ?bday. " +
					"?empl ex:salary ?salr. " +
					"?empl ex:webId ?wbid. " +
					"?empl ex:privateWebId ?pwid. " +
					"?empl ex:marriedTo ?spou. " +
					"?empl ex:department ?dept. " +
				"} " +
				"With " +
					"?empl = uri(ex:employee, '/', spy:urlEncode(?lastname), '/', spy:urlEncode(?firstname)) " +
					"?fnme = plainLiteral(concat(?firstname, ' ', ?lastname)) " +
					"?empt = typedLiteral(?employment_time, concat('http://www.w3.org/2001/XMLSchema#', ?employment_time_unit)) " +
					"?bday = typedLiteral(?birthday, xsd:date) " +
					"?salr = typedLiteral(" +
						"?salary_monthly + ?salary_monthly + ?salary_monthly " +
						"+ ?salary_monthly + ?salary_monthly + ?salary_monthly " +
						"+ ?salary_monthly + ?salary_monthly + ?salary_monthly " +
						"+ ?salary_monthly + ?salary_monthly + ?salary_monthly, xsd:int) " +
					"?wbid = uri('http://ex.org/', ?lastname, '/', ?firstname) " +
					"?pwid = uri(?web_id) " +
					"?spou = bNode(?id) " +
					"?dept = uri('http://ex.org/', ?id) " +
				"From " +
					"employee");
		
		deptView = vdf.create(
				"Create View departments As " +
				"Construct { " +
					"?dept a ex:Department. " +
					"?dept rdfs:label ?dnme. " +
					"?dept ex:title ?dttl." +
				"} " +
				"With " +
					"?dept = uri('http://ex.org/', ?id) " +
					"?dnme = plainLiteral(?default_name) " +
					"?dttl = plainLiteral(?name, ?lang) " +
				"From" +
					"[[ SELECT dept.id AS id, default_name, lang, name  FROM dept JOIN dept_translation ON dept.id=dept_translation.id ]]");
	}
	
	
	@Test
	public synchronized void test01() throws NotImplementedException, SQLException {
		String metricName = "test01";
		metric.setName(metricName);
		metric.setParentDimension("parent");
		metric.initMeasureDataSink();
		
		metric.assessViews(Arrays.asList(employeeView1, deptView));
		
		float expected = 10/(float) 13;
		assertEquals(expected, ((ValueTestingSink) sink).writtenValue(metricName), 0);
	}


	@Test
	public synchronized void test02() throws NotImplementedException, SQLException {
		String metricName = "test01";
		metric.setName(metricName);
		metric.setParentDimension("parent");
		metric.initMeasureDataSink();
		
		metric.assessViews(Arrays.asList(employeeView2, deptView));
		
		float expected = 21/(float) 13;
		assertEquals(expected, ((ValueTestingSink) sink).writtenValue(metricName), 0);
	}


	@Test
	public void test03() throws NotImplementedException, SQLException {
		metric.setCountVariablePredicates(false);
		String metricName = "test01";
		metric.setName(metricName);
		metric.setParentDimension("parent");
		metric.initMeasureDataSink();
		
		metric.assessViews(Arrays.asList(employeeView2, deptView));
		metric.setCountVariablePredicates(true);
		
		float expected = 11/(float) 13;
		assertEquals(expected, ((ValueTestingSink) sink).writtenValue(metricName), 0);
	}
}
