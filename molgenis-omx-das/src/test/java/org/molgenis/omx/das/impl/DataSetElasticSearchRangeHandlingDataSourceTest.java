package org.molgenis.omx.das.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mockito.Mockito;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.Query;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.GenomeConfig;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.search.Hit;
import org.molgenis.search.SearchResult;
import org.molgenis.util.ApplicationContextProvider;
import org.molgenis.util.HandleRequestDelegationException;
import org.springframework.context.ApplicationContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import uk.ac.ebi.mydas.configuration.DataSourceConfiguration;
import uk.ac.ebi.mydas.configuration.PropertyType;
import uk.ac.ebi.mydas.exceptions.BadReferenceObjectException;
import uk.ac.ebi.mydas.exceptions.CoordinateErrorException;
import uk.ac.ebi.mydas.exceptions.DataSourceException;
import uk.ac.ebi.mydas.exceptions.UnimplementedFeatureException;
import uk.ac.ebi.mydas.model.DasAnnotatedSegment;
import uk.ac.ebi.mydas.model.DasFeature;
import uk.ac.ebi.mydas.model.DasFeatureOrientation;
import uk.ac.ebi.mydas.model.DasMethod;
import uk.ac.ebi.mydas.model.DasPhase;
import uk.ac.ebi.mydas.model.DasTarget;
import uk.ac.ebi.mydas.model.DasType;

public class DataSetElasticSearchRangeHandlingDataSourceTest
{
	RepositoryRangeHandlingDataSource source;
	private DasFeature dasFeature;
	private DataService dataService;
	private ArrayList<Hit> resultList;
	private ArrayList<DasFeature> featureList;
	private GenomeConfig config;

	@BeforeMethod
	public void setUp() throws HandleRequestDelegationException, Exception
	{
		dataService = mock(DataService.class);
		config = mock(GenomeConfig.class);

		ApplicationContext ctx = mock(ApplicationContext.class);
		when(ctx.getBean(DataService.class)).thenReturn(dataService);
		when(ctx.getBean(GenomeConfig.class)).thenReturn(config);
		new ApplicationContextProvider().setApplicationContext(ctx);

		EntityMetaData metaData = new DefaultEntityMetaData("dataset");
		when(dataService.getEntityMetaData("dataset")).thenReturn(metaData);
		when(config.getAttributeNameForAttributeNameArray(config.GENOMEBROWSER_CHROM, metaData)).thenReturn("CHROM");

		DasType type = new DasType("0", "", "", "type");
		DasMethod method = new DasMethod("not_recorded", "not_recorded", "ECO:0000037");
		source = new RepositoryRangeHandlingDataSource();
		DataSourceConfiguration dataSourceConfig = mock(DataSourceConfiguration.class);
		PropertyType propertyType = new PropertyType();
		propertyType.setValue("type");
		when(dataSourceConfig.getDataSourceProperties()).thenReturn(Collections.singletonMap("type", propertyType));
		source.init(null, null, dataSourceConfig);
		Map<URL, String> linkout = new HashMap<URL, String>();
		linkout.put(new URL("http://www.molgenis.org/"), "Link");

		List<DasTarget> dasTarget = new ArrayList<DasTarget>();
		dasTarget.add(new MolgenisDasTarget("mutation id", 10, 1000, "mutation name,description"));
		List<String> notes = new ArrayList<String>();
		notes.add("track:dataset");
		notes.add("source:MOLGENIS");

		dasFeature = new DasFeature("mutation id", "mutation name,description", type, method, 10, 1000, new Double(0),
				DasFeatureOrientation.ORIENTATION_NOT_APPLICABLE, DasPhase.PHASE_NOT_APPLICABLE, notes, linkout,
				dasTarget, new ArrayList<String>(), null);

		Query q = new QueryImpl().eq("CHROM", "1");
		q.pageSize(100);
		SearchResult result = mock(SearchResult.class);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("STOP", 1000);
		map.put("linkout", "http://www.molgenis.org/");
		map.put("NAME", "mutation name");
		map.put("INFO", "description");
		map.put("POS", 10);
		map.put("ID", "mutation id");
		map.put("CHROM", "1");

		MapEntity entity = new MapEntity(map);
		resultList = new ArrayList<Hit>();
		resultList.add(new Hit("", "", map));
		featureList = new ArrayList<DasFeature>();
		featureList.add(dasFeature);
		when(dataService.findAll("dataset", q)).thenReturn(Arrays.<Entity> asList(entity));
		when(result.iterator()).thenReturn(resultList.iterator());

		when(config.getAttributeNameForAttributeNameArray(config.GENOMEBROWSER_CHROM, entity.getEntityMetaData()))
				.thenReturn("CHROM");
		when(config.getAttributeNameForAttributeNameArray(config.GENOMEBROWSER_START, entity.getEntityMetaData()))
				.thenReturn("POS");
		when(config.getAttributeNameForAttributeNameArray(config.GENOMEBROWSER_STOP, entity.getEntityMetaData()))
				.thenReturn("STOP");
		when(config.getAttributeNameForAttributeNameArray(config.GENOMEBROWSER_ID, entity.getEntityMetaData()))
				.thenReturn("ID");
		when(config.getAttributeNameForAttributeNameArray(config.GENOMEBROWSER_DESCRIPTION, entity.getEntityMetaData()))
				.thenReturn("INFO");
		when(config.getAttributeNameForAttributeNameArray(config.GENOMEBROWSER_NAME, entity.getEntityMetaData()))
				.thenReturn("NAME");
		when(config.getAttributeNameForAttributeNameArray(config.GENOMEBROWSER_LINK, entity.getEntityMetaData()))
				.thenReturn("linkout");

	}

	@AfterMethod
	public void teardown()
	{
		Mockito.reset(dataService);
	}

	@Test()
	public void getFeaturesRange() throws UnimplementedFeatureException, DataSourceException,
			BadReferenceObjectException, CoordinateErrorException
	{
		assertEquals(new DasAnnotatedSegment("1", 1, 100000, "1.00", "1", featureList).getFeatures(), source
				.getFeatures("1,dasdataset_dataset", 1, 100000, 100).getFeatures());
		assertEquals(new DasAnnotatedSegment("1", 1, 100000, "1.00", "1", featureList).getSegmentId(), source
				.getFeatures("1,dasdataset_dataset", 1, 100000, 100).getSegmentId());
		assertEquals(new DasAnnotatedSegment("1", 1, 100000, "1.00", "1", featureList).getStartCoordinate(), source
				.getFeatures("1,dasdataset_dataset", 1, 100000, 100).getStartCoordinate());
		assertEquals(new DasAnnotatedSegment("1", 1, 100000, "1.00", "1", featureList).getStopCoordinate(), source
				.getFeatures("1,dasdataset_dataset", 1, 100000, 100).getStopCoordinate());
	}

	@Test()
	public void getTypes() throws UnimplementedFeatureException, DataSourceException, BadReferenceObjectException,
			CoordinateErrorException
	{
		assertEquals(Collections.singleton(new DasType("type", null, "?", "type")), source.getTypes());
	}
}
