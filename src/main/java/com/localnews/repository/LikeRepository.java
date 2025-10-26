package com.localnews.repository;

import com.localnews.entity.Like;
import com.localnews.entity.Media;
import com.localnews.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByMediaAndUser(Media media, User user);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.media = :media AND l.type = 'LIKE'")
    long countLikesByMedia(@Param("media") Media media);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.media = :media AND l.type = 'DISLIKE'")
    long countDislikesByMedia(@Param("media") Media media);

    void deleteByMediaAndUser(Media media, User user);
}
