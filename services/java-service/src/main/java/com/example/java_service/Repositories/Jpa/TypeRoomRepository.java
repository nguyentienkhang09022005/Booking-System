package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.TypeRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeRoomRepository extends JpaRepository<TypeRoom, String>
{

}
