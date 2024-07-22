package com.project.services;

import com.project.exceptions.DuplicateEntityException;
import com.project.exceptions.EntityNotFoundException;
import com.project.helpers.PermissionHelper;
import com.project.models.Tag;
import com.project.models.User;
import com.project.repositories.contracts.TagRepository;
import com.project.services.contracts.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagServiceImpl implements TagService {
    public static final String USER_BLOCKED_ERROR = "You are blocked and cannot create tags!";
    private TagRepository tagRepository;

    @Autowired
    public TagServiceImpl(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    public Tag getTagById(int id) {
        return tagRepository.getTagById(id);
    }

    @Override
    public List<Tag> getAllTags() {
        return tagRepository.getAllTags();
    }

    @Override
    public void createTag(Tag tag, User user) {
        PermissionHelper.isBlocked(user, USER_BLOCKED_ERROR);
        boolean duplicateExist = true;

        try {
            tagRepository.getTagByName(tag.getTag());
        } catch (EntityNotFoundException e) {
            duplicateExist = false;
        }

        if (duplicateExist) {
            throw new DuplicateEntityException("Tag", "name", tag.getTag());
        }
        tagRepository.createTag(tag);
    }

    @Override
    public void updateTag(Tag tag) {

    }

    @Override
    public void deleteTag(int id) {

    }

    @Override
    public Tag getTagByName(String name) {
        return tagRepository.getTagByName(name);
    }
}
