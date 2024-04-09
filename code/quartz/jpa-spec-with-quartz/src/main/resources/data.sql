-- Classes
INSERT INTO Member_Class (id, name)
VALUES (101, 'Spin');
INSERT INTO Member_Class (id, name)
VALUES (102, 'Tennis');
INSERT INTO Member_Class (id, name)
VALUES (103, 'Basketball');
INSERT INTO Member_Class (id, name)
VALUES (104, 'FitCore 2000');
INSERT INTO Member_Class (id, name)
VALUES (105, 'Swimming');
INSERT INTO Member_Class (id, name)
VALUES (106, 'New Class');

-- Members
INSERT INTO Member (id, first_name, last_name, zip_code, active, interests)
VALUES (1, 'Jon', 'Anderson', '90215', true, 'I like to write music and play racket sports');
INSERT INTO Member (id, first_name, last_name, zip_code, active, interests)
VALUES (2, 'Trevor', 'Rabin', '90215', true, 'I am a guitar-playing point guard, who likes tennis too');
INSERT INTO Member (id, first_name, last_name, zip_code, active, interests)
VALUES (3, 'Rick', 'Wakeman', '02215', true, 'I enjoy a good practical joke');
INSERT INTO Member (id, first_name, last_name, zip_code, active, interests)
VALUES (4, 'Chris', 'Squire', '33756', false, '');
INSERT INTO Member (id, first_name, last_name, zip_code, active, interests)
VALUES (5, 'Alan', 'White', '90210', false, 'I have no interests');
INSERT INTO Member (id, first_name, last_name, zip_code, active, interests)
VALUES (6, 'Geddy', 'Lee', '90212', true, 'I enjoy playing racquetball too');
INSERT INTO Member (id, first_name, last_name, zip_code, active, interests)
VALUES (7, 'Alex', 'Lifeson', '90211', true, 'I like staying in shape, drinking games');
INSERT INTO Member (id, first_name, last_name, zip_code, active, interests)
VALUES (8, 'Neil', 'Peart', '10010', false, 'I enjoy cycling, writing and playing drums');
INSERT INTO Member (id, first_name, last_name, zip_code, active, interests)
VALUES (9, 'Rik', 'Emmett', '10020', true, 'I enjoy playing guitar, but wanna work out');
INSERT INTO Member (id, first_name, last_name, zip_code, active, interests)
VALUES (10, 'Mike', 'Levine', '10022', true, 'Playing bass is my passion, but I do great at FitCore 2000 too');

-- Member_Class
INSERT INTO Member_Class_Cross_Ref(member_id, class_id)
VALUES (1, 102);
INSERT INTO Member_Class_Cross_Ref(member_id, class_id)
VALUES (1, 105);
INSERT INTO Member_Class_Cross_Ref(member_id, class_id)
VALUES (2, 103);
INSERT INTO Member_Class_Cross_Ref(member_id, class_id)
VALUES (3, 103);
INSERT INTO Member_Class_Cross_Ref(member_id, class_id)
VALUES (4, 101);
INSERT INTO Member_Class_Cross_Ref(member_id, class_id)
VALUES (4, 102);
INSERT INTO Member_Class_Cross_Ref(member_id, class_id)
VALUES (4, 105);
INSERT INTO Member_Class_Cross_Ref(member_id, class_id)
VALUES (5, 104);
INSERT INTO Member_Class_Cross_Ref(member_id, class_id)
VALUES (6, 102);
INSERT INTO Member_Class_Cross_Ref(member_id, class_id)
VALUES (7, 104);
INSERT INTO Member_Class_Cross_Ref(member_id, class_id)
VALUES (7, 105);
INSERT INTO Member_Class_Cross_Ref(member_id, class_id)
VALUES (8, 101);
INSERT INTO Member_Class_Cross_Ref(member_id, class_id)
VALUES (8, 102);
INSERT INTO Member_Class_Cross_Ref(member_id, class_id)
VALUES (8, 105);
INSERT INTO Member_Class_Cross_Ref(member_id, class_id)
VALUES (10, 104);