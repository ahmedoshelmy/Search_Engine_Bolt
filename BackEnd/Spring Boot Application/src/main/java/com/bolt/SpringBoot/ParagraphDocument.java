package com.bolt.SpringBoot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "ParagraphsCollection")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParagraphDocument {
    @Id
    private int id;
    private String paragraph;
}
