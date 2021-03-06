/*
 * Copyright (c) 2012. The Genome Analysis Centre, Norwich, UK
 * MISO project contacts: Robert Davey @ TGAC
 * *********************************************************************
 *
 * This file is part of MISO.
 *
 * MISO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MISO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MISO. If not, see <http://www.gnu.org/licenses/>.
 *
 * *********************************************************************
 */

package uk.ac.bbsrc.tgac.miso.webapp.controller;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import com.eaglegenomics.simlims.core.User;
import com.eaglegenomics.simlims.core.manager.SecurityManager;

import uk.ac.bbsrc.tgac.miso.core.data.Issue;
import uk.ac.bbsrc.tgac.miso.core.data.Project;
import uk.ac.bbsrc.tgac.miso.core.data.Subproject;
import uk.ac.bbsrc.tgac.miso.core.data.impl.ProjectImpl;
import uk.ac.bbsrc.tgac.miso.core.data.impl.TargetedSequencing;
import uk.ac.bbsrc.tgac.miso.core.manager.IssueTrackerManager;
import uk.ac.bbsrc.tgac.miso.core.security.util.LimsSecurityUtils;
import uk.ac.bbsrc.tgac.miso.core.util.LimsUtils;
import uk.ac.bbsrc.tgac.miso.dto.Dtos;
import uk.ac.bbsrc.tgac.miso.service.ProjectService;
import uk.ac.bbsrc.tgac.miso.service.ReferenceGenomeService;
import uk.ac.bbsrc.tgac.miso.service.SubprojectService;
import uk.ac.bbsrc.tgac.miso.service.TargetedSequencingService;
import uk.ac.bbsrc.tgac.miso.service.security.AuthorizationManager;
import uk.ac.bbsrc.tgac.miso.webapp.context.ExternalUriBuilder;

@Controller
@RequestMapping("/project")
@SessionAttributes("project")
public class EditProjectController {
  private static final Logger log = LoggerFactory.getLogger(EditProjectController.class);

  @Autowired
  private AuthorizationManager authorizationManager;
  @Autowired
  private SecurityManager securityManager;
  @Autowired
  private ProjectService projectService;
  @Autowired
  private IssueTrackerManager issueTrackerManager;
  @Autowired
  private ExternalUriBuilder externalUriBuilder;
  @Autowired
  private ReferenceGenomeService referenceGenomeService;
  @Autowired
  private TargetedSequencingService targetedSequencingService;
  @Autowired
  private SubprojectService subprojectService;

  public void setProjectService(ProjectService projectService) {
    this.projectService = projectService;
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    CustomDateEditor cde = new CustomDateEditor(LimsUtils.getDateFormat(), true);
    binder.registerCustomEditor(Date.class, cde);
  }

  @ModelAttribute("maxLengths")
  public Map<String, Integer> maxLengths() throws IOException {
    return projectService.getProjectColumnSizes();
  }

  @GetMapping("/new")
  public ModelAndView setupForm(ModelMap model) throws IOException {
    return setupForm(ProjectImpl.UNSAVED_ID, model);
  }

  @GetMapping("/shortname/{shortName}")
  public ModelAndView byProjectShortName(@PathVariable String shortName, ModelMap model) throws IOException {
    Project project = projectService.getProjectByShortName(shortName);
    if (project == null) throw new NotFoundException("No project found for shortname " + shortName);
    return setupForm(project.getId(), model);
  }

  @GetMapping("/{projectId}")
  public ModelAndView setupForm(@PathVariable Long projectId, ModelMap model) throws IOException {
    List<Issue> issues = Collections.emptyList();
    User user = authorizationManager.getCurrentUser();
    Project project = null;
    if (projectId == ProjectImpl.UNSAVED_ID) {
      project = new ProjectImpl(user);
      model.put("title", "New Project");
    } else {
      project = projectService.get(projectId);
      if (project == null) {
        throw new NotFoundException("No project found for ID " + projectId.toString());
      }
      Collection<Subproject> subprojects = subprojectService.getByProjectId(projectId);
      model.put("subprojects", Dtos.asSubprojectDtos(subprojects));
      model.put("title", "Project " + projectId);
      try {
        issues = issueTrackerManager.getIssuesByTag(project.getShortName());
      } catch (IOException e) {
        log.error("Error retrieving issues", e);
      }
    }

    model.put("projectIssues", issues.stream().map(Dtos::asDto).collect(Collectors.toList()));
    model.put("projectReportLinks", externalUriBuilder.getUris(project));
    model.put("referenceGenome", referenceGenomeService.listAllReferenceGenomeTypes());

    Collection<TargetedSequencing> targetedSequencingList = targetedSequencingService.list();
    targetedSequencingList.add(TargetedSequencing.NULL);
    model.put("targetedSequencing", targetedSequencingList);
    model.put("formObj", project);
    model.put("project", project);
    model.put("owners", LimsSecurityUtils.getPotentialOwners(user, project, securityManager.listAllUsers()));
    model.put("accessibleUsers", LimsSecurityUtils.getAccessibleUsers(user, project, securityManager.listAllUsers()));
    model.put("accessibleGroups", LimsSecurityUtils.getAccessibleGroups(user, project, securityManager.listAllGroups()));

    return new ModelAndView("/pages/editProject.jsp", model);
  }

  @PostMapping
  public ModelAndView processSubmit(@ModelAttribute("project") Project project, ModelMap model, SessionStatus session,
      HttpServletRequest request)
      throws IOException {
    try {
      projectService.saveProject(project);
      session.setComplete();
      model.clear();
      return new ModelAndView("redirect:/miso/project/" + project.getId(), model);
    } catch (IOException ex) {
      if (log.isDebugEnabled()) {
        log.debug("Failed to save project", ex);
      }
      throw ex;
    }
  }
}
