package ch.qos.logback.access.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.qos.logback.access.dummy.DummyRequest;
import ch.qos.logback.access.dummy.DummyResponse;
import ch.qos.logback.access.dummy.DummyServerAdapter;
import ch.qos.logback.access.spi.AccessContext;
import ch.qos.logback.access.spi.AccessEvent;
import ch.qos.logback.core.db.DriverManagerConnectionSource;
import ch.qos.logback.core.util.StatusPrinter;

public class DBAppenderTest  {

  AccessContext context;
  DBAppender appender;
  DriverManagerConnectionSource connectionSource;

  static DBAppenderTestFixture DB_APPENDER_TEST_FIXTURE;
  
  @BeforeClass
  static public void fixtureSetUp() throws SQLException {
    DB_APPENDER_TEST_FIXTURE = new DBAppenderTestFixture();
    DB_APPENDER_TEST_FIXTURE.setUp();
  } 
  
  @AfterClass
  static public  void fixtureTearDown()  throws SQLException {
    DB_APPENDER_TEST_FIXTURE.tearDown();
  }
  
  @Before
  public void setUp() throws SQLException {
    context = new AccessContext();
    context.setName("default");
    appender = new DBAppender();
    appender.setName("DB");
    appender.setContext(context);
    connectionSource = new DriverManagerConnectionSource();
    connectionSource.setContext(context);
    connectionSource.setDriverClass(DBAppenderTestFixture.DRIVER_CLASS);
    connectionSource.setUrl(DB_APPENDER_TEST_FIXTURE.url);
    connectionSource.setUser(DB_APPENDER_TEST_FIXTURE.user);
    connectionSource.setPassword(DB_APPENDER_TEST_FIXTURE.password);
    connectionSource.start();
    appender.setConnectionSource(connectionSource);
  }
  
  private void setInsertHeadersAndStart(boolean insert) {
    appender.setInsertHeaders(insert);
    appender.start();
  }

  @After
  public void tearDown() throws SQLException {
    context = null;
    appender = null;
    connectionSource = null;
  }

  @Test
  public void testAppendAccessEvent() throws SQLException {
    setInsertHeadersAndStart(false);

    AccessEvent event = createAccessEvent();
    appender.append(event);
    
    Statement stmt = connectionSource.getConnection().createStatement();
    ResultSet rs = null;
    rs = stmt.executeQuery("SELECT * FROM access_event");
    if (rs.next()) {
      assertEquals(event.getTimeStamp(), rs.getLong(1));
      assertEquals(event.getRequestURI(), rs.getString(2));
      assertEquals(event.getRequestURL(), rs.getString(3));
      assertEquals(event.getRemoteHost(), rs.getString(4));
      assertEquals(event.getRemoteUser(), rs.getString(5));
      assertEquals(event.getRemoteAddr(), rs.getString(6));
      assertEquals(event.getProtocol(), rs.getString(7));
      assertEquals(event.getMethod(), rs.getString(8));
      assertEquals(event.getServerName(), rs.getString(9));
      assertEquals(event.getRequestContent(), rs.getString(10));
    } else {
      fail("No row was inserted in the database");
    }

    rs.close();
    stmt.close();
  }
  
  
  @Test
  public void testCheckNoHeadersAreInserted() throws Exception {
    setInsertHeadersAndStart(false);
    
    AccessEvent event = createAccessEvent();
    appender.append(event);
    StatusPrinter.print(context.getStatusManager());
    
    //Check that no headers were inserted
    Statement stmt = connectionSource.getConnection().createStatement();
    ResultSet rs = null;
    rs = stmt.executeQuery("SELECT * FROM access_event_header");
    
    assertFalse(rs.next());
    rs.close();
    stmt.close();
  }

  @Test
  public void testAppendHeaders() throws SQLException {   
    setInsertHeadersAndStart(true);
    
    AccessEvent event = createAccessEvent();
    appender.append(event);

    Statement stmt = connectionSource.getConnection().createStatement();
    ResultSet rs = null;
    rs = stmt.executeQuery("SELECT * FROM access_event_header");
    String key;
    String value;
    if (!rs.next()) {
      fail("There should be results to this query");
    } else {
      key = rs.getString(2);
      value = rs.getString(3);
      assertNotNull(key);
      assertNotNull(value);
      assertEquals(event.getRequestHeader(key), value);
      rs.next();
      key = rs.getString(2);
      value = rs.getString(3);
      assertNotNull(key);
      assertNotNull(value);
      assertEquals(event.getRequestHeader(key), value);
    }
    if (rs.next()) {
      fail("There should be no more rows available");
    }

    rs.close();
    stmt.close();
  }

  @Test
  public void testAppendMultipleEvents() throws SQLException {
    String uri = "testAppendMultipleEvents";
    for (int i = 0; i < 10; i++) {
      AccessEvent event = createAccessEvent(uri);
      appender.append(event);
    }

    Statement stmt = connectionSource.getConnection().createStatement();
    ResultSet rs = null;
    rs = stmt.executeQuery("SELECT * FROM access_event where requestURI='"+uri+"'");
    int count = 0;
    while (rs.next()) {
      count++;
    }
    assertEquals(10, count);

    rs.close();
    stmt.close();
  }

  private AccessEvent createAccessEvent() {
     return createAccessEvent(""); 
  }
  
  private AccessEvent createAccessEvent(String uri) {
    DummyRequest request = new DummyRequest();
    request.setRequestUri(uri);
    DummyResponse response = new DummyResponse();
    DummyServerAdapter adapter = new DummyServerAdapter(request, response);

    AccessEvent ae = new AccessEvent(request, response, adapter);
    return ae;
  }
}