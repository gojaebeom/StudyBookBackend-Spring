package me.studybook.api.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.studybook.api.domain.*;
import me.studybook.api.dto.req.ReqPostCreateDto;
import me.studybook.api.dto.req.ReqPostsDto;
import me.studybook.api.dto.res.ResPostDetailDto;
import me.studybook.api.dto.res.ResPostsDto;
import me.studybook.api.repo.post.PostRepo;
import me.studybook.api.repo.post.PostTagRepo;
import me.studybook.api.repo.post.TagRepo;
import me.studybook.api.repo.post.mapper.PostDetailMapper;
import me.studybook.api.repo.post.mapper.PostsMapper;
import me.studybook.api.repo.user.UserRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.net.BindException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class PostService {

    private PostRepo postRepo;
    private TagRepo tagRepo;
    private UserRepo userRepo;
    private PostTagRepo postTagRepo;

    public List<ResPostsDto> index(ReqPostsDto postsDto) throws Exception {

        List<PostsMapper> posts = null;



        if(postsDto.getSort().equals("recent")){
            posts = postRepo.findAllByTitleIsLikeOrderByUpdatedAtDesc("%"+postsDto.getKeyword()+"%");
        }
        else if(postsDto.getSort().equals("old")){
            posts = postRepo.findAllByTitleIsLikeOrderByUpdatedAtAsc("%"+postsDto.getKeyword()+"%");
        }
        else if(postsDto.getSort().equals("views")){
            posts = postRepo.findAllByTitleIsLikeOrderByViewsDesc("%"+postsDto.getKeyword()+"%");
        }

        return ResPostsDto.of(posts);
    }

    public List<ResPostsDto> index(Long id) throws Exception{

        List<PostsMapper> _posts = postRepo.findAllByUserId(id);
        List<PostTag> postTags = postTagRepo.findAll();

        return ResPostsDto.of(_posts, postTags);
    }

    public ResPostDetailDto show(Long id) throws Exception {

        Optional<Post> _post = postRepo.findById(id);
        if(_post.isEmpty()){
            throw new Exception("게시물이 존재하지 않습니다");
        }
        Post post = _post.get();
        System.out.println(post.getViews());
        if(post.getViews() == null){
            post.setViews(1L);
        }else{
            post.setViews(post.getViews()+1);
        }

        PostDetailMapper postDetail =  postRepo.findOneById(id);

        List<PostTag> postTags = postTagRepo.findByPostId(id);

        return ResPostDetailDto.of(postDetail, postTags);
    }


    public void create(ReqPostCreateDto postCreateDto) throws Exception {

        Long userId = postCreateDto.getUserId();
        User user = userRepo.findUserById(userId);

        if(postCreateDto.getTitle() == null) {
            throw new BindException("재목을 입력해주세요.");
        }

        List<String> _tags = postCreateDto.getTags();
        if(_tags == null){
            throw new BindException("태그는 한개 이상 등록해야합니다.");
        }
        if(_tags.size() > 3){
            throw new BindException("태그는 3개까지 등록할 수 있습니다.");
        }

        Set checkedTags = new HashSet(_tags);

        if(_tags.size() != checkedTags.size()){
            throw new BindException("하나의 게시물에 동일한 태그를 등록할 수 없습니다.");
        }

        Post post = postCreateDto.toPost(user);
        List<Tag> tags = postCreateDto.toTags();
        List<PostTag> postTags = postCreateDto.toPostTags(post, tags);


        postRepo.save(post);
        tagRepo.saveAll(tags);
        postTagRepo.saveAll(postTags);
    }
}
