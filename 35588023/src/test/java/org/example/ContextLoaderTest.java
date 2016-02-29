package org.example;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockRequestDispatcher;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import javax.servlet.Filter;
import javax.servlet.RequestDispatcher;

@ContextConfiguration(loader = TestWebContextLoader.class, locations = { "classpath:spring-context.xml", "classpath:spring-security-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class ContextLoaderTest
{
  @Autowired
  private Filter springSecurityFilterChain;

  @Autowired
  private WebApplicationContext webApplicationContext;

  private MockMvc mock;

  @Before
  public void setUp()
  {
    mock = MockMvcBuilders.webApplicationContextSetup(webApplicationContext).build();
  }

  @Test
  public void testContextLoads()
  {
  }
}

class TestWebContextLoader extends AbstractContextLoader
{
  protected final MockServletContext servletContext;

  public TestWebContextLoader()
  {
    this.servletContext = initServletContext("src/main/resources", new FileSystemResourceLoader());
  }

  private MockServletContext initServletContext(String warRootDir, ResourceLoader resourceLoader)
  {
    return new MockServletContext(warRootDir, resourceLoader)
    {
      // Required for DefaultServletHttpRequestHandler...
      public RequestDispatcher getNamedDispatcher(String path)
      {
        return (path.equals("default")) ? new MockRequestDispatcher(path) : super.getNamedDispatcher(path);
      }
    };
  }

  public ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception
  {
    GenericWebApplicationContext context = new GenericWebApplicationContext();
    context.getEnvironment().setActiveProfiles(mergedConfig.getActiveProfiles());
    prepareContext(context);
    loadBeanDefinitions(context, mergedConfig);
    return context;
  }

  public ApplicationContext loadContext(String... locations) throws Exception
  {
    // should never be called
    throw new UnsupportedOperationException();
  }

  protected void prepareContext(GenericWebApplicationContext context)
  {
    this.servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
    context.setServletContext(this.servletContext);
  }

  protected void loadBeanDefinitions(GenericWebApplicationContext context, String[] locations)
  {
    new XmlBeanDefinitionReader(context).loadBeanDefinitions(locations);
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    context.registerShutdownHook();
  }

  protected void loadBeanDefinitions(GenericWebApplicationContext context, MergedContextConfiguration mergedConfig)
  {
    new AnnotatedBeanDefinitionReader(context).register(mergedConfig.getClasses());
    loadBeanDefinitions(context, mergedConfig.getLocations());
  }

  @Override
  protected String getResourceSuffix()
  {
    return "-context.xml";
  }
}
