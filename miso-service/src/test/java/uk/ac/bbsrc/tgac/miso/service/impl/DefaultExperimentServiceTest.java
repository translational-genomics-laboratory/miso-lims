package uk.ac.bbsrc.tgac.miso.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.eaglegenomics.simlims.core.SecurityProfile;

import uk.ac.bbsrc.tgac.miso.core.data.Experiment;
import uk.ac.bbsrc.tgac.miso.core.data.InstrumentModel;
import uk.ac.bbsrc.tgac.miso.core.data.impl.LibraryImpl;
import uk.ac.bbsrc.tgac.miso.core.data.impl.StudyImpl;
import uk.ac.bbsrc.tgac.miso.core.security.SecurableByProfile;
import uk.ac.bbsrc.tgac.miso.core.service.naming.NamingScheme;
import uk.ac.bbsrc.tgac.miso.core.service.naming.validation.ValidationResult;
import uk.ac.bbsrc.tgac.miso.core.store.ExperimentStore;
import uk.ac.bbsrc.tgac.miso.core.store.SecurityProfileStore;
import uk.ac.bbsrc.tgac.miso.core.store.SecurityStore;
import uk.ac.bbsrc.tgac.miso.service.KitService;
import uk.ac.bbsrc.tgac.miso.service.LibraryService;
import uk.ac.bbsrc.tgac.miso.service.InstrumentModelService;
import uk.ac.bbsrc.tgac.miso.service.StudyService;
import uk.ac.bbsrc.tgac.miso.service.security.AuthorizationManager;

public class DefaultExperimentServiceTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  private final Experiment experiment = new Experiment();

  @Mock
  private ExperimentStore experimentStore;
  @Mock
  private InstrumentModelService platformService;
  @Mock
  private LibraryService libraryService;
  @Mock
  private StudyService studyService;
  @Mock
  private SecurityStore securityStore;
  @Mock
  private SecurityProfileStore securityProfileStore;
  @Mock
  private KitService kitService;
  @Mock
  private AuthorizationManager authorizationManager;
  @Mock
  private NamingScheme namingScheme;
  @InjectMocks
  private DefaultExperimentService sut;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    Mockito.when(namingScheme.validateName(Matchers.anyString())).thenReturn(ValidationResult.success());
    experiment.setInstrumentModel(new InstrumentModel());
    experiment.getInstrumentModel().setId(2L);
    experiment.setLibrary(new LibraryImpl());
    experiment.getLibrary().setId(3L);
    experiment.setSecurityProfile(new SecurityProfile());
    experiment.getSecurityProfile().setProfileId(4L);
    experiment.setStudy(new StudyImpl());
    experiment.getStudy().setId(5L);
  }

  @Test
  public void testSaveExperiment() throws IOException {
    final long expectedReturn = 1L;
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {

        return null;
      }
    }).when(authorizationManager).throwIfNotWritable(any(SecurableByProfile.class));

    when(experimentStore.save(experiment)).thenReturn(expectedReturn);
    when(platformService.get(experiment.getInstrumentModel().getId())).thenReturn(experiment.getInstrumentModel());
    when(libraryService.get(experiment.getLibrary().getId())).thenReturn(experiment.getLibrary());
    when(studyService.get(experiment.getStudy().getId())).thenReturn(experiment.getStudy());
    when(securityProfileStore.get(experiment.getSecurityProfile().getProfileId())).thenReturn(experiment.getSecurityProfile());

    assertEquals(expectedReturn, sut.save(experiment));
    verify(experimentStore, times(2)).save(experiment);
  }

  @Test
  public void testSaveExperimentThrows() throws IOException {
    doThrow(new IOException()).when(authorizationManager).throwIfNotWritable(any(Experiment.class));

    thrown.expect(IOException.class);
    sut.save(experiment);

    verify(experimentStore, never()).save(experiment);
  }

  @Test
  public void testGetExperimentById() throws IOException {
    long inputId = 1L;
    when(experimentStore.get(inputId)).thenReturn(experiment);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {

        return null;
      }
    }).when(authorizationManager).throwIfNotReadable(any(SecurableByProfile.class));

    assertEquals(experiment, sut.get(inputId));

    verify(experimentStore).get(inputId);
  }

  @Test
  public void testGetExperimentByIdThrows() throws IOException {
    long inputId = 1L;
    when(experimentStore.get(inputId)).thenReturn(experiment);
    doThrow(IOException.class).when(authorizationManager).throwIfNotReadable(any(SecurableByProfile.class));

    thrown.expect(IOException.class);

    sut.get(inputId);

    verify(experimentStore).get(inputId);
  }
}
