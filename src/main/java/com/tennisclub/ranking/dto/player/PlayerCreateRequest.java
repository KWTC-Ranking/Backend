package com.tennisclub.ranking.dto.player;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlayerCreateRequest(@NotBlank @Size(max = 150) String fullName, @Email String email) {}
