create database upload_base;

use upload_base;

drop table if exists file_upload_record;

create table if not exists file_upload_record(
                                                 id bigint auto_increment primary key comment '主键',
                                                 file_md5 varchar(64) not null COMMENT '文件 MD5',
    bucket varchar(64) not null comment '存放的桶名称',
    object_name varchar(256) not null COMMENT '上传成功的对象全路径名',
    file_size bigint comment '文件大小',
    create_time datetime default current_timestamp comment '创建时间',
    update_time datetime default current_timestamp on update current_timestamp comment '更新时间',

    unique key uk_file_md5(file_md5),
    key idx_create_time(create_time)
    );

