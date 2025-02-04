package org.motechproject.cmslite;

import org.junit.Test;
import org.motechproject.cmslite.api.CMSLiteService;
import org.motechproject.cmslite.api.ResourceNotFoundException;
import org.motechproject.cmslite.api.ResourceQuery;
import org.motechproject.cmslite.api.dao.CMSLiteResources;
import org.motechproject.cmslite.api.impl.CMSLiteServiceImpl;
import org.motechproject.cmslite.api.model.Resource;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class CMSLiteServiceTest {

    @Test
    public void shouldReturnInputStreamToContentIfContentExists() throws IOException, ResourceNotFoundException {
        ResourceQuery query = new ResourceQuery("name", "language");
        CMSLiteResources mockResources = mock(CMSLiteResources.class);
        CMSLiteService cmsLiteService = new CMSLiteServiceImpl(mockResources);
        Resource resource = new Resource();
        InputStream inputStreamToResource = mock(InputStream.class);

        resource.setInputStream(inputStreamToResource);
        when(mockResources.getResource(query)).thenReturn(resource);

        InputStream content = cmsLiteService.getContent(query);

        verify(mockResources).getResource(query);
        assertEquals(inputStreamToResource, content);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void shouldThrowExceptionIfContentDoesNotExist() throws ResourceNotFoundException {
        ResourceQuery query = new ResourceQuery("test1", "language");
        CMSLiteResources mockResources = mock(CMSLiteResources.class);
        CMSLiteService cmsLiteService = new CMSLiteServiceImpl(mockResources);

        when(mockResources.getResource(query)).thenReturn(null);

        cmsLiteService.getContent(query);

        fail("Should have thrown ResourceNotFoundException when query is null");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenQueryIsNull() throws ResourceNotFoundException {
        CMSLiteResources mockResources = mock(CMSLiteResources.class);
        CMSLiteService cmsLiteService = new CMSLiteServiceImpl(mockResources);

        cmsLiteService.getContent(null);
        verify(mockResources,never()).getResource(null);

        fail("Should have thrown IllegalArgumentException when query is null");
    }

}
