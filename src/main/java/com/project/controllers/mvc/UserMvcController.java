package com.project.controllers.mvc;

import com.project.exceptions.AuthenticationException;
import com.project.exceptions.DuplicateEntityException;
import com.project.exceptions.EntityNotFoundException;
import com.project.exceptions.UnauthorizedOperationException;
import com.project.helpers.AuthenticationHelper;
import com.project.helpers.MapperHelper;
import com.project.models.Avatar;
import com.project.models.FilteredUsersOptions;
import com.project.models.PhoneNumber;
import com.project.models.User;
import com.project.models.dtos.FilterUserDto;
import com.project.models.dtos.PhoneNumberDto;
import com.project.models.dtos.UserDto;
import com.project.services.contracts.AvatarService;
import com.project.services.contracts.PhoneService;
import com.project.services.contracts.RoleService;
import com.project.services.contracts.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/ti/users")
public class UserMvcController {

    public static final String YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE = "You dont have access to this page";
    public static final String INVALID_INPUT = "Invalid input";
    public static final String PHONE_NUMBER_MUST_BE_ONLY_DIGITS = "Phone number must be only digits";
    private final UserService userService;
    private final AuthenticationHelper authenticationHelper;
    private final MapperHelper mapperHelper;
    private final AvatarService avatarService;
    private final PhoneService phoneService;
    private final RoleService roleService;

    @Autowired
    public UserMvcController(UserService userService, AuthenticationHelper authenticationHelper, MapperHelper mapperHelper, AvatarService avatarService, PhoneService phoneService, RoleService roleService) {
        this.userService = userService;
        this.authenticationHelper = authenticationHelper;
        this.mapperHelper = mapperHelper;
        this.avatarService = avatarService;
        this.phoneService = phoneService;
        this.roleService = roleService;
    }

    @ModelAttribute("isAdmin")
    public boolean populateIsAdmin(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            User user = authenticationHelper.tryGetUserFromSession(session);
            if (user.getRole().getRoleId() == 3) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @ModelAttribute("isBlocked")
    public boolean populateIsBlocked(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            User user = authenticationHelper.tryGetUserFromSession(session);
            if (user.isBlocked()) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @ModelAttribute("isModerator")
    public boolean populateIsModerator(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            User user = authenticationHelper.tryGetUserFromSession(session);
            if (user.getRole().equals(roleService.getRoleById(2))) {
                return true;
            }
        }
        return false;
    }

    @ModelAttribute("isAuthenticated")
    public boolean populateIsAuthenticated(HttpSession session) {
        return session.getAttribute("currentUser") != null;
    }

    @GetMapping("/edit")
    public String showEditUserPage(Model model, HttpSession session) {
        try {
            User currentUser = authenticationHelper.tryGetUserFromSession(session);
            model.addAttribute("user", currentUser);
            model.addAttribute("userId", currentUser.getId());
            model.addAttribute("isAdmin", currentUser.getRole().getRoleId() == 3);
            model.addAttribute("phoneNumber", currentUser.getPhoneNumbers());
            return "EditUserView";
        } catch (AuthenticationException e) {
            return "redirect:/ti/auth/login";
        }
    }

    @PostMapping("/edit")
    public String handleEditUser(
            @Valid @ModelAttribute("user") UserDto userToBeEdited,
            BindingResult bindingResult, HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "EditUserView";
        }

        try {
            User currentUser = authenticationHelper.tryGetUserFromSession(session);
            User updatedUser = mapperHelper.updateUserFromDto(userToBeEdited, currentUser.getId());
            userService.update(currentUser, updatedUser);
            return "redirect:/ti";
        } catch (Exception e) {
            bindingResult.rejectValue("username", "error.user", e.getMessage());
            return "EditUserView";
        }
    }

    @PostMapping("/edit/avatar")
    public String uploadAvatar(@RequestParam("avatarFile") MultipartFile avatarFile, HttpSession session, Model model) {
        try {
            User user = authenticationHelper.tryGetUserFromSession(session);
            Avatar avatar = avatarService.uploadAvatar(user, avatarFile);
            model.addAttribute("avatarUrl", avatar.getAvatar());
            return "redirect:/ti/users/edit";
        } catch (Exception e) {
            return "ErrorView";
        }
    }

    @GetMapping("/admin")
    public String showAdminPortal(HttpSession session, Model model) {
        try {
            User user = authenticationHelper.tryGetUserFromSession(session);
            if (user.getRole().getRoleId() == 3 || user.getRole().getRoleId() == 2) {
                return "AdminPanelView";
            }
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        } catch (AuthenticationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        } catch (UnauthorizedOperationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        }
    }

    @GetMapping("/admin/users")
    public String showAllUsers(@ModelAttribute("filterUsersOptions") FilterUserDto filterUserDto,
                               @RequestParam(defaultValue = "0") int page, // Параметър за страницата
                               @RequestParam(defaultValue = "5") int size, // Параметър за размера на страницата
                               Model model, HttpSession session) {
        try {
            User user = authenticationHelper.tryGetUserFromSession(session);

            if (user.getRole().getRoleId() == 1) {
                model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
                model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
                return "ErrorView";
            }

            if (user.getRole().getRoleId() == 3 || user.getRole().getRoleId() == 2) {
                FilteredUsersOptions filteredUsersOptions = new FilteredUsersOptions(
                        filterUserDto.getUsername(),
                        filterUserDto.getFirstName(),
                        filterUserDto.getEmail(),
                        filterUserDto.getSortBy(),
                        filterUserDto.getSortOrder()
                );
                List<User> users = userService.getAllUsers(user, filteredUsersOptions, page, size);
                model.addAttribute("filterUsersOptions", filterUserDto);
                model.addAttribute("users", users);
                model.addAttribute("currentPage", page); // Добавяме текущата страница
                model.addAttribute("pageSize", size); // Добавяме размера на страницата
                return "AllUsersView";
            }
            return "ErrorView";
        } catch (AuthenticationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        } catch (UnauthorizedOperationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        }
    }

    @GetMapping("/{id}")
    public String showUserDetails(@PathVariable int id, Model model, HttpSession session) {
        try {
            User user = authenticationHelper.tryGetUserFromSession(session);
            User userToDisplay = userService.getUserById(user, id);
            model.addAttribute("user", userToDisplay);
            return "UserDetailsView";
        } catch (UnauthorizedOperationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (AuthenticationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        }
    }

    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable int id, Model model, HttpSession session) {
        try {
            User user = authenticationHelper.tryGetUserFromSession(session);
            if (user.getRole().getRoleId() == 3 || user.getRole().getRoleId() == 2) {
                userService.deleteUser(user, id);
                return "redirect:/ti/users/admin/users";
            }
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        } catch (UnauthorizedOperationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (AuthenticationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        }
    }

    @PostMapping("/admin/users/{id}/block")
    public String blockUser(@PathVariable int id, Model model, HttpSession session) {
        try {
            User user = authenticationHelper.tryGetUserFromSession(session);
            if (user.getRole().getRoleId() == 3 || user.getRole().getRoleId() == 2) {
                userService.blockUser(user, id);
                return "redirect:/ti/users/" + id;
            }
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        } catch (UnauthorizedOperationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (AuthenticationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        }
    }

    @PostMapping("/admin/users/{id}/unblock")
    public String unblockUser(@PathVariable int id, Model model, HttpSession session) {
        try {
            User user = authenticationHelper.tryGetUserFromSession(session);
            if (user.getRole().getRoleId() == 3 || user.getRole().getRoleId() == 2) {
                userService.unblocked(user, id);
                return "redirect:/ti/users/" + id;
            }
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        } catch (UnauthorizedOperationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (AuthenticationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        }
    }

    @PostMapping("/admin/users/{id}/promote")
    public String promoteUser(@PathVariable int id, Model model, HttpSession session) {
        try {
            User user = authenticationHelper.tryGetUserFromSession(session);
            User userToBePromoted = userService.getUserById(user, id);
            if (user.getRole().getRoleId() == 3) {
                userService.userToBeAdmin(userToBePromoted);
                return "redirect:/ti/users/" + id;
            }
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        } catch (UnauthorizedOperationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (AuthenticationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        }
    }

    @PostMapping("/admin/users/{id}/demote")
    public String demoteUser(@PathVariable int id, Model model, HttpSession session) {
        try {
            User user = authenticationHelper.tryGetUserFromSession(session);
            User userToBeDemoted = userService.getUserById(user, id);
            if (user.getRole().getRoleId() == 3) {
                userService.userToBeDemoted(userToBeDemoted);
                return "redirect:/ti/users/" + id;
            }
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        } catch (UnauthorizedOperationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (AuthenticationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        }
    }

    @PostMapping("/admin/{id}/phone")
    public String addPhone(@PathVariable int id, @ModelAttribute("phoneNumber") @Valid PhoneNumberDto phoneNumberDto,
                           BindingResult bindingResult, Model model, HttpSession session) {
        User user = authenticationHelper.tryGetUserFromSession(session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("statusCode", INVALID_INPUT);
            model.addAttribute("error", PHONE_NUMBER_MUST_BE_ONLY_DIGITS);
            return "ErrorView";
        }
        try {

            PhoneNumber phoneNumber = mapperHelper.getFromPhoneDto(phoneNumberDto);
            if (user.getRole().getRoleId() == 3) {
                phoneService.addPhoneToAnAdmin(user, phoneNumber);
                return "redirect:/ti/users/" + id;
            }
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        } catch (UnauthorizedOperationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (AuthenticationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        } catch (DuplicateEntityException e) {
            model.addAttribute("statusCode", HttpStatus.CONFLICT.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (Exception e) {
            model.addAttribute("statusCode", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        }
    }

    @PostMapping("/admin/{id}/phone/{phoneId}/remove")
    public String removePhone(@PathVariable int id, @PathVariable int phoneId,
                              Model model, HttpSession session) {
        try {
            User user = authenticationHelper.tryGetUserFromSession(session);
            if (user.getRole().getRoleId() == 3 || user.getId() == id) {
                phoneService.removePhoneFromAdmin(user, phoneId);
                return "redirect:/ti/users/" + id;
            }
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        } catch (UnauthorizedOperationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (AuthenticationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        }
    }

    @GetMapping("/{id}/posts")
    public String showUserPosts(@PathVariable int id, Model model, HttpSession session) {
        try {
            User user = authenticationHelper.tryGetUserFromSession(session);
            User userToDisplay = userService.getUserById(user, id);
            model.addAttribute("user", userToDisplay);
            return "UserAllPostsView";
        } catch (UnauthorizedOperationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (AuthenticationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        }
    }

    @GetMapping("/{id}/comments")
    public String showUserComments(@PathVariable int id, Model model, HttpSession session) {
        try {
            User user = authenticationHelper.tryGetUserFromSession(session);
            User userToDisplay = userService.getUserById(user, id);
            model.addAttribute("user", userToDisplay);
            return "UserAllCommentsView";
        } catch (UnauthorizedOperationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (AuthenticationException e) {
            model.addAttribute("statusCode", HttpStatus.FORBIDDEN.getReasonPhrase());
            model.addAttribute("error", YOU_DONT_HAVE_ACCESS_TO_THIS_PAGE);
            return "ErrorView";
        }
    }
}