ALTER TABLE ` student_score ` DROP FOREIGN KEY ` fk_学生 `;
ALTER TABLE ` student_score ` DROP FOREIGN KEY ` fk_成绩_课程_1 `;

DROP TABLE ` student `;
DROP TABLE ` course `;
DROP TABLE ` student_score `;
DROP TABLE ` table_1 `;

CREATE TABLE ` student `
(
    `id` int
(
    11
) NOT NULL AUTO_INCREMENT,
    ` name ` varchar
(
    255
) NULL,
    ` age ` int
(
    255
) NULL,
    PRIMARY KEY
(
    `id`
)
    );

CREATE TABLE ` course `
(
    `id` int
(
    11
) NOT NULL AUTO_INCREMENT,
    ` name ` varchar
(
    255
) NULL,
    PRIMARY KEY
(
    `id`
)
    );

CREATE TABLE ` student_score `
(
    `id` int
(
    11
) NOT NULL,
    ` student_id ` int
(
    11
) NULL,
    ` course_id ` int
(
    11
) NULL,
    ` score ` int
(
    255
) NULL,
    PRIMARY KEY
(
    `id`
)
    );

CREATE TABLE ` table_1 `
(
);


ALTER TABLE ` student_score ` ADD CONSTRAINT ` fk_学生 ` FOREIGN KEY (` student_id `) REFERENCES ` student ` (` id `);
ALTER TABLE ` student_score ` ADD CONSTRAINT ` fk_成绩_课程_1 ` FOREIGN KEY (` course_id `) REFERENCES ` course ` (` id `);

