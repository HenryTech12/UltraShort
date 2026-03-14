package org.app.UltraShort.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class URL {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private String urlID;
    private String url;
    private String shortUrl;

}
