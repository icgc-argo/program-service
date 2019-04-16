PROTO_DIR := src/main/argo-proto
PROTO_SENTINEL := $(PROTO_DIR)/.git
NUM_PROTO_FILES := $$(ls -1 $(PROTO_DIR) | wc -l)
BASHRC_FILE := ${HOME}/.bashrc

.PHONY: all

all: init-submodules


$(PROTO_SENTINEL):
	$(call make_header, "Initializing submodules...")
	@git submodule init
	@git submodule update --recursive
	@echo "   -- done"

init-submodules: $(PROTO_SENTINEL)

