package com.byteblogs.helloblog.posts.service.impl;

import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.byteblogs.common.base.domain.Result;
import com.byteblogs.common.base.domain.vo.BaseVO;
import com.byteblogs.common.base.domain.vo.UserSessionVO;
import com.byteblogs.common.constant.Constants;
import com.byteblogs.common.constant.ErrorConstants;
import com.byteblogs.common.util.ExceptionUtil;
import com.byteblogs.common.util.HttpClientDownloader;
import com.byteblogs.common.util.JsonUtil;
import com.byteblogs.common.util.Markdown2HtmlUtil;
import com.byteblogs.common.util.PageUtil;
import com.byteblogs.common.util.PreviewTextUtils;
import com.byteblogs.common.util.SessionUtil;
import com.byteblogs.helloblog.category.dao.TagsDao;
import com.byteblogs.helloblog.category.domain.po.Tags;
import com.byteblogs.helloblog.category.domain.vo.TagsVO;
import com.byteblogs.helloblog.posts.dao.PostsAttributeDao;
import com.byteblogs.helloblog.posts.dao.PostsDao;
import com.byteblogs.helloblog.posts.dao.PostsTagsDao;
import com.byteblogs.helloblog.posts.domain.po.Posts;
import com.byteblogs.helloblog.posts.domain.po.PostsAttribute;
import com.byteblogs.helloblog.posts.domain.po.PostsTags;
import com.byteblogs.helloblog.posts.domain.vo.CNBlogsVO;
import com.byteblogs.helloblog.posts.domain.vo.CSDNVO;
import com.byteblogs.helloblog.posts.domain.vo.JianShuVO;
import com.byteblogs.helloblog.posts.domain.vo.JueJinVO;
import com.byteblogs.helloblog.posts.domain.vo.PostsVO;
import com.byteblogs.helloblog.posts.domain.vo.SegmentFaultVO;
import com.byteblogs.helloblog.posts.service.PostsService;
import com.byteblogs.system.enums.PlatformEnum;
import com.overzealous.remark.Remark;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.model.OOSpider;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author byteblogs
 * @since 2019-08-28
 */
@Service
@Slf4j
public class PostsServiceImpl extends ServiceImpl<PostsDao, Posts> implements PostsService {

    @Autowired
    private PostsDao postsDao;

    @Autowired
    private PostsAttributeDao postsAttributeDao;

    @Autowired
    private TagsDao tagsDao;

    @Autowired
    private PostsTagsDao postsTagsDao;

    @Override
    public Result savePosts(PostsVO postsVO) {

        boolean flag = postsVO == null || postsVO.getStatus() == null || StringUtils.isBlank(postsVO.getTitle()) || StringUtils.isBlank(postsVO.getContent());
        if (flag) {
            ExceptionUtil.rollback("新增文章参数异常", ErrorConstants.PARAM_INCORRECT);
        }

        UserSessionVO userSessionInfo = SessionUtil.getUserSessionInfo();
        String html = Markdown2HtmlUtil.html(postsVO.getContent());

        Posts posts = new Posts();
        posts.setTitle(postsVO.getTitle()).setCreateTime(LocalDateTime.now()).setUpdateTime(LocalDateTime.now()).setThumbnail(postsVO.getThumbnail());
        posts.setStatus(postsVO.getStatus()).setSummary(PreviewTextUtils.getText(html, 126)).setAuthorId(userSessionInfo.getId());
        postsDao.insert(posts);

        postsAttributeDao.insert(new PostsAttribute().setContent(postsVO.getContent()).setPostsId(posts.getId()));

        List<TagsVO> tagsList = postsVO.getTagsList();

        if (!CollectionUtils.isEmpty(tagsList)) {
            tagsList.forEach(tagsVO -> {
                if (tagsVO.getId() == null) {
                    // add
                    Tags tags = new Tags().setName(tagsVO.getName()).setCreateTime(LocalDateTime.now()).setUpdateTime(LocalDateTime.now());
                    this.tagsDao.insert(tags);
                    tagsVO.setId(tags.getId());
                }

                postsTagsDao.insert(new PostsTags().setPostsId(posts.getId()).setTagsId(tagsVO.getId()).setCreateTime(LocalDateTime.now()).setUpdateTime(LocalDateTime.now()));
            });
        }

        if (postsVO.getIsPublishByteBlogs().equals(Constants.YES)) {
            String result = HttpUtil.post(Constants.BYTE_BLOGS_ADD_ARTICLE, JsonUtil.toJsonString(new PostsVO().setTitle(postsVO.getTitle()).setContent(postsVO.getContent()).setSocialId(userSessionInfo.getSocialId())));
            Result result1 = JsonUtil.parseObject(result, Result.class);
            log.debug("保存到ByteBlogs草稿箱 {}", result);
            if (result1.getSuccess() == Constants.YES) {
                this.postsDao.update(new Posts().setSyncStatus(Constants.YES), new LambdaUpdateWrapper<Posts>().eq(Posts::getId, posts.getId()));
                return Result.createWithSuccessMessage("文章保存成功，并且同步到ByteBlogs草稿箱，请点击" + Constants.BYTE_BLOGS_URL + "/editor/posts" + "进行编辑");
            }
        }

        return Result.createWithSuccessMessage();
    }

    @Override
    public Result updatePosts(PostsVO postsVO) {

        boolean flag = postsVO == null || postsVO.getId() == null || postsVO.getStatus() == null || StringUtils.isBlank(postsVO.getTitle()) || StringUtils.isBlank(postsVO.getContent());
        if (flag) {
            ExceptionUtil.rollback("新增文章参数异常", ErrorConstants.PARAM_INCORRECT);
        }

        UserSessionVO userSessionInfo = SessionUtil.getUserSessionInfo();
        String html = Markdown2HtmlUtil.html(postsVO.getContent());

        Posts posts1 = this.postsDao.selectOne(new LambdaQueryWrapper<Posts>().eq(Posts::getId, postsVO.getId()));
        if (posts1 == null) {
            ExceptionUtil.rollback("", ErrorConstants.DATA_NO_EXIST);
        }

        posts1.setTitle(postsVO.getTitle()).setUpdateTime(LocalDateTime.now()).setThumbnail(postsVO.getThumbnail());
        posts1.setStatus(postsVO.getStatus()).setSummary(PreviewTextUtils.getText(html, 126)).setAuthorId(userSessionInfo.getId());

        this.postsDao.updateById(posts1);
        this.postsAttributeDao.update(new PostsAttribute().setContent(postsVO.getContent()), new LambdaUpdateWrapper<PostsAttribute>().eq(PostsAttribute::getPostsId, posts1.getId()));

        List<TagsVO> tagsList = postsVO.getTagsList();

        if (!CollectionUtils.isEmpty(tagsList)) {
            List<PostsTags> originalList = this.postsTagsDao.selectList(new LambdaQueryWrapper<PostsTags>().eq(PostsTags::getPostsId, posts1.getId()));
            List<PostsTags> categoryTagsList = originalList.stream().filter(postsTags -> !postsVO.getTagsList().stream().map(BaseVO::getId).collect(Collectors.toList())
                    .contains(postsTags.getTagsId())).collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(categoryTagsList)) {
                categoryTagsList.forEach(categoryTags -> {
                    this.postsTagsDao.deleteById(categoryTags.getId());
                });
            }

            tagsList.forEach(tagsVO -> {
                if (tagsVO.getId() == null) {
                    // add
                    Tags tags = new Tags().setName(tagsVO.getName()).setCreateTime(LocalDateTime.now()).setUpdateTime(LocalDateTime.now());
                    this.tagsDao.insert(tags);
                    tagsVO.setId(tags.getId());
                    postsTagsDao.insert(new PostsTags().setPostsId(posts1.getId()).setTagsId(tagsVO.getId()).setCreateTime(LocalDateTime.now()).setUpdateTime(LocalDateTime.now()));
                } else {
                    PostsTags postsTags = this.postsTagsDao.selectOne(new LambdaQueryWrapper<PostsTags>().eq(PostsTags::getTagsId, tagsVO.getId()));
                    if (postsTags == null) {
                        postsTagsDao.insert(new PostsTags().setPostsId(posts1.getId()).setTagsId(tagsVO.getId()).setCreateTime(LocalDateTime.now()).setUpdateTime(LocalDateTime.now()));
                    }
                }
            });
        } else {

            postsTagsDao.delete(new LambdaQueryWrapper<PostsTags>().eq(PostsTags::getPostsId, posts1.getId()));
        }

        return Result.createWithSuccessMessage();
    }

    @Override
    public Result deletePosts(Long id) {

        if (id == null) {
            ExceptionUtil.rollback("", ErrorConstants.PARAM_INCORRECT);
        }

        Posts posts = this.postsDao.selectById(id);
        if (posts == null) {
            ExceptionUtil.rollback("", ErrorConstants.DATA_NO_EXIST);
        }

        this.postsDao.deleteById(id);
        this.postsAttributeDao.delete(new LambdaUpdateWrapper<PostsAttribute>().eq(PostsAttribute::getPostsId, id));
        this.postsTagsDao.delete(new LambdaUpdateWrapper<PostsTags>().eq(PostsTags::getPostsId, id));

        return Result.createWithSuccessMessage();
    }

    @Override
    public Result getPosts(Long id) {

        if (id == null) {
            ExceptionUtil.rollback("", ErrorConstants.PARAM_INCORRECT);
        }

        Posts posts = this.postsDao.selectById(id);

        if (posts == null) {
            ExceptionUtil.rollback("", ErrorConstants.DATA_NO_EXIST);
        }

        PostsVO postsVO = new PostsVO();
        postsVO.setCreateTime(posts.getCreateTime()).setSummary(posts.getSummary()).setTitle(posts.getTitle()).setId(posts.getId()).setThumbnail(posts.getThumbnail()).setIsComment(posts.getIsComment());

        PostsAttribute postsAttribute = this.postsAttributeDao.selectOne(new LambdaQueryWrapper<PostsAttribute>().eq(PostsAttribute::getPostsId, posts.getId()));
        postsVO.setContent(postsAttribute.getContent()).setViews(posts.getViews()).setComments(posts.getComments());

        List<PostsTags> postsTagsList = postsTagsDao.selectList(new LambdaQueryWrapper<PostsTags>().eq(PostsTags::getPostsId, posts.getId()));
        List<TagsVO> tagsVOList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(postsTagsList)) {
            postsTagsList.forEach(postsTags -> {
                Tags tags = this.tagsDao.selectById(postsTags.getTagsId());
                tagsVOList.add(new TagsVO().setId(tags.getId()).setName(tags.getName()));
            });
        }

        postsVO.setTagsList(tagsVOList);

        this.postsDao.incrementView(posts.getId());
        return Result.createWithModel(postsVO);
    }

    @Override
    public Result<PostsVO> getPostsList(PostsVO postsVO) {
        postsVO = Optional.ofNullable(postsVO).orElse(new PostsVO());

        Page page = Optional.ofNullable(PageUtil.checkAndInitPage(postsVO)).orElse(PageUtil.initPage());
        if (StringUtils.isNotBlank(postsVO.getKeywords())) {
            postsVO.setKeywords("%" + postsVO.getKeywords() + "%");
        }
        List<PostsVO> postsVOList = this.postsDao.selectPostsList(page, postsVO.getArchiveDate(), postsVO.getKeywords());

        if (!CollectionUtils.isEmpty(postsVOList)) {
            postsVOList.forEach(postsVO1 -> {
                List<PostsTags> postsTagsList = postsTagsDao.selectList(new LambdaQueryWrapper<PostsTags>().eq(PostsTags::getPostsId, postsVO1.getId()));
                List<TagsVO> tagsVOList = new ArrayList<>();
                if (!CollectionUtils.isEmpty(postsTagsList)) {
                    postsTagsList.forEach(postsTags -> {
                        Tags tags = this.tagsDao.selectById(postsTags.getTagsId());
                        tagsVOList.add(new TagsVO().setId(tags.getId()).setName(tags.getName()));
                    });
                }
                postsVO1.setTagsList(tagsVOList);
            });
        }

        return Result.createWithPaging(postsVOList, PageUtil.initPageInfo(page));
    }

    @Override
    public Result getPlatformArticle(PostsVO postsVO) {

        if (postsVO == null || postsVO.getPlatformType() == null || StringUtils.isBlank(postsVO.getSourceUri())) {
            ExceptionUtil.rollback("", ErrorConstants.PARAM_INCORRECT);
        }
        crawler(postsVO);
        return Result.createWithModel(postsVO);
    }

    @Override
    public Result getArchiveTotalByDateList(PostsVO postsVO) {

        Page page = Optional.ofNullable(PageUtil.checkAndInitPage(postsVO)).orElse(PageUtil.initPage());
        List<PostsVO> postsVOList = this.postsDao.selectArchiveTotalGroupDateList(page);

        return Result.createWithModels(postsVOList);
    }

    @Override
    public Result updatePostsStatus(PostsVO postsVO) {

        if (postsVO == null) {
            ExceptionUtil.rollback("", ErrorConstants.PARAM_INCORRECT);
        }
        this.postsDao.updateById(new Posts().setId(postsVO.getId()).setStatus(postsVO.getStatus()).setUpdateTime(LocalDateTime.now()));
        return Result.createWithSuccessMessage();
    }

    @Override
    public Result publishByteBlogs(PostsVO postsVO) {

        if (postsVO == null || postsVO.getId() == null) {
            ExceptionUtil.rollback("", ErrorConstants.PARAM_INCORRECT);
        }
        UserSessionVO userSessionInfo = SessionUtil.getUserSessionInfo();
        Posts posts = this.postsDao.selectById(postsVO.getId());
        if (posts == null) {
            ExceptionUtil.rollback("数据不存在", ErrorConstants.DATA_NO_EXIST);
        }

        PostsAttribute postsAttribute = this.postsAttributeDao.selectOne(new LambdaQueryWrapper<PostsAttribute>().eq(PostsAttribute::getPostsId, posts.getId()));
        String params = JsonUtil.toJsonString(new PostsVO().setTitle(posts.getTitle()).setContent(postsAttribute.getContent()).setSocialId(userSessionInfo.getSocialId()));
        log.debug("params {}", params);
        String result = HttpUtil.post(Constants.BYTE_BLOGS_ADD_ARTICLE, params);
        Result result1 = JsonUtil.parseObject(result, Result.class);
        log.debug("保存到ByteBlogs草稿箱 {}", result);
        if (result1.getSuccess() == Constants.YES) {
            this.postsDao.update(new Posts().setSyncStatus(Constants.YES), new LambdaUpdateWrapper<Posts>().eq(Posts::getId, postsVO.getId()));
            return Result.createWithSuccessMessage("同步到ByteBlogs成功，请点击" + Constants.BYTE_BLOGS_URL + "/editor/posts" + "进行编辑");
        }

        return result1;
    }

    private void crawler(PostsVO postsVO) {
        Class platformClass = PlatformEnum.getEnumTypeMap().get(postsVO.getPlatformType()).getPlatformClass();
        Spider spider = OOSpider.create(Site.me(), platformClass).setDownloader(new HttpClientDownloader());
        Object object = spider.get(postsVO.getSourceUri());

        String join = "";
        if (postsVO.getPlatformType().equals(PlatformEnum.JIAN_SHU.getType())) {
            JianShuVO jianShuVO = (JianShuVO) object;
            postsVO.setTitle(jianShuVO.getTitle());
            join = String.join("", jianShuVO.getContent());
        } else if (postsVO.getPlatformType().equals(PlatformEnum.JUE_JIN.getType())) {
            JueJinVO jueJinVO = (JueJinVO) object;
            postsVO.setTitle(jueJinVO.getTitle());
            join = String.join("", jueJinVO.getContent());
        } else if (postsVO.getPlatformType().equals(PlatformEnum.SEGMENT_FAULT.getType())) {
            SegmentFaultVO segmentFaultVO = (SegmentFaultVO) object;
            postsVO.setTitle(segmentFaultVO.getTitle());
            join = String.join("", segmentFaultVO.getContent());
        } else if (postsVO.getPlatformType().equals(PlatformEnum.CSDN.getType())) {
            CSDNVO csdnVO = (CSDNVO) object;
            postsVO.setTitle(csdnVO.getTitle());
            join = String.join("", csdnVO.getContent());
        } else if (postsVO.getPlatformType().equals(PlatformEnum.CN_BLOGS.getType())) {
            CNBlogsVO cnBlogsVO = (CNBlogsVO) object;
            postsVO.setTitle(cnBlogsVO.getTitle());
            join = String.join("", cnBlogsVO.getContent());
        } else {
            ExceptionUtil.rollback("", ErrorConstants.PARAM_INCORRECT);
        }
        String converted = new Remark().convertFragment(join);
        postsVO.setContent(converted);
    }
}
