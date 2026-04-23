package org.app.UltraShort.model;

import jakarta.persistence.*;
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
    @Column(unique = true, nullable = false, length = 2048)
    private String url;
    private String shortUrl;

}
